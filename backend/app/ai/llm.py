"""AI text-model interface. The real implementation speaks the OpenAI-compatible chat API, so
any hosted or self-hosted open-source model works (OpenRouter, Together, Groq, vLLM, Ollama).
The model is chosen by settings, never hard-coded."""

import logging
from dataclasses import dataclass
from typing import Any, Protocol

import httpx

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class LLMRequest:
    model: str
    system: str
    prompt: str
    max_tokens: int = 4096
    json_mode: bool = True
    temperature: float = 0.3


@dataclass(frozen=True)
class LLMResponse:
    text: str
    input_tokens: int
    output_tokens: int


class LLMError(Exception):
    pass


class LLMProvider(Protocol):
    def complete(self, request: LLMRequest) -> LLMResponse: ...


class FakeLLMProvider:
    """Returns canned responses (in order, or from [responder]) and records every request."""

    def __init__(
        self,
        responses: list[str] | None = None,
        responder: Any = None,
    ) -> None:
        self._responses = list(responses or [])
        self._responder = responder
        self.requests: list[LLMRequest] = []

    def complete(self, request: LLMRequest) -> LLMResponse:
        self.requests.append(request)
        if self._responder is not None:
            text = str(self._responder(request))
        else:
            text = self._responses.pop(0) if self._responses else "{}"
        return LLMResponse(
            text=text, input_tokens=len(request.prompt) // 4, output_tokens=len(text) // 4
        )


class OpenAICompatibleLLMProvider:
    """Calls `POST {base_url}/chat/completions` with a bearer key."""

    def __init__(self, base_url: str, api_key: str, timeout: float = 120.0) -> None:
        self._url = base_url.rstrip("/") + "/chat/completions"
        self._headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        self._timeout = timeout

    def complete(self, request: LLMRequest) -> LLMResponse:
        body: dict[str, Any] = {
            "model": request.model,
            "messages": [
                {"role": "system", "content": request.system},
                {"role": "user", "content": request.prompt},
            ],
            "max_tokens": request.max_tokens,
            "temperature": request.temperature,
        }
        if request.json_mode:
            body["response_format"] = {"type": "json_object"}
        try:
            r = httpx.post(self._url, json=body, headers=self._headers, timeout=self._timeout)
            r.raise_for_status()
            data = r.json()
            text = data["choices"][0]["message"]["content"] or ""
        except (httpx.HTTPError, KeyError, IndexError, ValueError) as exc:
            raise LLMError(f"LLM call failed: {exc}") from exc
        usage = data.get("usage") or {}
        return LLMResponse(
            text=text,
            input_tokens=int(usage.get("prompt_tokens") or 0),
            output_tokens=int(usage.get("completion_tokens") or 0),
        )
