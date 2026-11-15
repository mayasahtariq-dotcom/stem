"""The OpenAI-compatible client: model fallbacks and term-key slugs (no network)."""

import json

import httpx
import pytest

from app.ai.llm import LLMError, LLMRequest, OpenAICompatibleLLMProvider
from app.ai.schemas import slugify


def provider_with(handler: object) -> OpenAICompatibleLLMProvider:
    p = OpenAICompatibleLLMProvider("https://example.test/v1", "key", retry_wait=0)
    p._client = httpx.Client(transport=httpx.MockTransport(handler))  # type: ignore[arg-type]
    return p


def reply(text: str) -> httpx.Response:
    return httpx.Response(
        200,
        json={
            "choices": [{"message": {"content": text}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 3, "completion_tokens": 5},
        },
    )


def test_falls_back_to_the_next_model_when_busy_or_empty() -> None:
    tried: list[str] = []

    def handler(request: httpx.Request) -> httpx.Response:
        model = json.loads(request.content)["model"]
        tried.append(model)
        if model == "busy:free":
            return httpx.Response(429, json={"error": "rate limited"})
        if model == "thinker:free":
            return reply("")  # spent the budget thinking
        return reply('{"ok": true}')

    p = provider_with(handler)
    r = p.complete(LLMRequest(model="busy:free, thinker:free, good:free", system="s", prompt="p"))
    assert r.text == '{"ok": true}'
    assert tried == ["busy:free", "thinker:free", "good:free"]
    assert (r.input_tokens, r.output_tokens) == (3, 5)


def test_raises_when_every_model_fails() -> None:
    p = provider_with(lambda request: httpx.Response(503))
    with pytest.raises(LLMError) as err:
        p.complete(LLMRequest(model="a,b", system="s", prompt="p"))
    assert "a:" in str(err.value) and "b:" in str(err.value)


def test_sends_json_mode_and_bearer_key() -> None:
    seen: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["auth"] = request.headers["Authorization"]
        seen["body"] = json.loads(request.content)
        return reply("{}")

    provider_with(handler).complete(LLMRequest(model="m", system="s", prompt="p"))
    body = seen["body"]
    assert isinstance(body, dict)
    assert seen["auth"] == "Bearer key"
    assert body["response_format"] == {"type": "json_object"}


@pytest.mark.parametrize(
    "raw,expected",
    [
        ("Newton's first law", "newtons_first_law"),
        ("kg m/s", "kg_m_s"),
        ("cell-wall", "cell_wall"),
        ("Mitochondria", "mitochondria"),
    ],
)
def test_term_keys_are_clean_slugs(raw: str, expected: str) -> None:
    assert slugify(raw) == expected
