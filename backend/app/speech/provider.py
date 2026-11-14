"""Speech provider interface (Sarvam now, swappable for Bhashini). M0 ships the fake only."""

from dataclasses import dataclass
from typing import Literal, Protocol

Language = Literal["hi", "kn", "en"]


@dataclass(frozen=True)
class SynthesisResult:
    audio: bytes
    sample_rate_hz: int
    media_type: str


@dataclass(frozen=True)
class Transcript:
    text: str
    language: Language
    confidence: float


class SpeechProvider(Protocol):
    def synthesize(
        self, text: str, language: Language, *, voice: str = "default", sample_rate_hz: int = 22050
    ) -> SynthesisResult: ...

    def transcribe(
        self, audio: bytes, language: Language, *, sample_rate_hz: int
    ) -> Transcript: ...


class FakeSpeechProvider:
    """Deterministic, offline speech provider for tests and debug builds."""

    def __init__(self, transcript: str = "") -> None:
        self._transcript = transcript
        self.synthesized: list[tuple[str, Language]] = []

    def synthesize(
        self, text: str, language: Language, *, voice: str = "default", sample_rate_hz: int = 22050
    ) -> SynthesisResult:
        self.synthesized.append((text, language))
        # 100 ms of 16-bit mono silence.
        silence = b"\x00\x00" * (sample_rate_hz // 10)
        return SynthesisResult(audio=silence, sample_rate_hz=sample_rate_hz, media_type="audio/L16")

    def transcribe(self, audio: bytes, language: Language, *, sample_rate_hz: int) -> Transcript:
        return Transcript(text=self._transcript, language=language, confidence=1.0)
