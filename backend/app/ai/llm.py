"""AI text-model interface. The real implementation speaks the OpenAI-compatible chat API, so
any hosted or self-hosted open-source model works (OpenRouter, Together, Groq, vLLM, Ollama).
The model is chosen by settings, never hard-coded."""

import logging
import time
from dataclasses import dataclass
from typing import Any, Protocol

import httpx

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class LLMRequest:
    model: str
    system: str
    prompt: str
    # Generous: reasoning models think before answering, and lessons are long JSON.
    max_tokens: int = 12000
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


def _ssl_context() -> Any:
    """Uses the operating system's certificate store (truststore), so HTTPS works behind
    antivirus or network filters that add their own root certificate; falls back to certifi."""
    try:
        import ssl

        import truststore

        return truststore.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    except ImportError:  # pragma: no cover
        return True


RETRYABLE = {408, 409, 425, 429, 500, 502, 503, 504}


class OpenAICompatibleLLMProvider:
    """Calls `POST {base_url}/chat/completions` with a bearer key.

    `request.model` may be a comma-separated list. Each model is tried in order, moving on
    when one is busy (rate-limited, server error) or returns an empty reply. Free hosted models
    are often rate-limited, so a list of free models keeps the pipeline working.
    """

    def __init__(
        self, base_url: str, api_key: str, timeout: float = 120.0, retry_wait: float = 2.0
    ) -> None:
        self._url = base_url.rstrip("/") + "/chat/completions"
        self._headers = {"Authorization": f"Bearer {api_key}", "Content-Type": "application/json"}
        self._timeout = timeout
        self._retry_wait = retry_wait
        self._client = httpx.Client(verify=_ssl_context(), timeout=timeout)

    def complete(self, request: LLMRequest) -> LLMResponse:
        models = [m.strip() for m in request.model.split(",") if m.strip()]
        errors: list[str] = []
        for attempt, model in enumerate(models):
            if attempt:
                time.sleep(self._retry_wait)
            try:
                return self._call(model, request)
            except LLMError as exc:
                errors.append(f"{model}: {exc}")
                log.warning(
                    "model failed, trying the next one",
                    extra={"model": model, "error": str(exc)[:200]},
                )
        raise LLMError("; ".join(errors) or "no model configured")

    def _call(self, model: str, request: LLMRequest) -> LLMResponse:
        body: dict[str, Any] = {
            "model": model,
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
            r = self._client.post(self._url, json=body, headers=self._headers)
        except httpx.HTTPError as exc:
            raise LLMError(f"network error: {exc}") from exc
        if r.status_code >= 400:
            kind = "busy" if r.status_code in RETRYABLE else "error"
            raise LLMError(f"HTTP {r.status_code} ({kind}): {r.text[:200]}")
        try:
            data = r.json()
            choice = data["choices"][0]
            text = choice["message"].get("content") or ""
        except (KeyError, IndexError, ValueError) as exc:
            raise LLMError(f"unexpected reply: {exc}") from exc
        if not text.strip():
            # Reasoning models can spend the whole budget thinking and return nothing.
            raise LLMError(f"empty reply (finish_reason={choice.get('finish_reason')})")
        usage = data.get("usage") or {}
        return LLMResponse(
            text=text,
            input_tokens=int(usage.get("prompt_tokens") or 0),
            output_tokens=int(usage.get("completion_tokens") or 0),
        )
