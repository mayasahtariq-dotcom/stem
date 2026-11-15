"""Speech provider interface (Sarvam now, swappable for Bhashini). Sarvam lives in
app/speech/sarvam.py; the fake is used in tests and whenever no key is configured."""

import io
import struct
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
    model: str

    def voice_for(self, language: Language) -> str: ...

    def synthesize(
        self, text: str, language: Language, *, voice: str = "default", sample_rate_hz: int = 22050
    ) -> SynthesisResult: ...

    def transcribe(
        self, audio: bytes, language: Language, *, sample_rate_hz: int
    ) -> Transcript: ...


class FakeSpeechProvider:
    """Deterministic, offline speech provider for tests and debug builds."""

    model = "fake"

    def __init__(self, transcript: str = "") -> None:
        self._transcript = transcript
        self.synthesized: list[tuple[str, Language]] = []

    def voice_for(self, language: Language) -> str:
        return "fake"

    def synthesize(
        self, text: str, language: Language, *, voice: str = "default", sample_rate_hz: int = 22050
    ) -> SynthesisResult:
        self.synthesized.append((text, language))
        return SynthesisResult(
            audio=silent_wav(sample_rate_hz), sample_rate_hz=sample_rate_hz, media_type="audio/wav"
        )

    def transcribe(self, audio: bytes, language: Language, *, sample_rate_hz: int) -> Transcript:
        return Transcript(text=self._transcript, language=language, confidence=1.0)


def silent_wav(sample_rate_hz: int = 22050, millis: int = 100) -> bytes:
    """A short, playable WAV file of silence (16-bit mono)."""
    frames = b"\x00\x00" * (sample_rate_hz * millis // 1000)
    buf = io.BytesIO()
    buf.write(b"RIFF" + struct.pack("<I", 36 + len(frames)) + b"WAVE")
    buf.write(
        b"fmt " + struct.pack("<IHHIIHH", 16, 1, 1, sample_rate_hz, sample_rate_hz * 2, 2, 16)
    )
    buf.write(b"data" + struct.pack("<I", len(frames)) + frames)
    return buf.getvalue()
