"""AI provider interface. M0 ships the fake only; the Claude implementation arrives in M2."""

from dataclasses import dataclass, field
from typing import Protocol


@dataclass(frozen=True)
class LLMImage:
    media_type: str
    data: bytes


@dataclass(frozen=True)
class LLMRequest:
    model: str
    system: str
    prompt: str
    images: list[LLMImage] = field(default_factory=list)
    max_tokens: int = 2048


@dataclass(frozen=True)
class LLMResponse:
    text: str
    input_tokens: int
    output_tokens: int


class LLMProvider(Protocol):
    def complete(self, request: LLMRequest) -> LLMResponse: ...

    def embed(self, texts: list[str]) -> list[list[float]]: ...


class FakeLLMProvider:
    """Returns canned responses and records every request. Makes no network calls."""

    def __init__(self, responses: list[str] | None = None, embedding_dim: int = 8) -> None:
        self._responses = list(responses or [])
        self._embedding_dim = embedding_dim
        self.requests: list[LLMRequest] = []

    def complete(self, request: LLMRequest) -> LLMResponse:
        self.requests.append(request)
        text = self._responses.pop(0) if self._responses else "{}"
        return LLMResponse(text=text, input_tokens=len(request.prompt), output_tokens=len(text))

    def embed(self, texts: list[str]) -> list[list[float]]:
        return [
            [float((hash(t) >> i) & 0xFF) / 255.0 for i in range(self._embedding_dim)]
            for t in texts
        ]
