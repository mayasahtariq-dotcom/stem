"""Sarvam text-to-speech (Bulbul). Checked against Sarvam's docs on 2026-11-15:

    POST {base}/text-to-speech   header: api-subscription-key
    {"text", "language_code": "hi-IN", "model": "bulbul:v4-flash", "speaker",
     "output_audio_codec": "mp3"}  ->  {"audios": ["<base64>"]}

Text is limited to 2,500 characters per request. Speech-to-text arrives in M7.
"""

import base64
import binascii
from typing import Any

import httpx

from app.speech.provider import Language, SynthesisResult, Transcript

LANGUAGE_CODES: dict[str, str] = {"hi": "hi-IN", "kn": "kn-IN", "en": "en-IN"}


class SpeechError(Exception):
    pass


def _ssl_context() -> Any:
    try:
        import ssl

        import truststore

        return truststore.SSLContext(ssl.PROTOCOL_TLS_CLIENT)
    except ImportError:  # pragma: no cover
        return True


class SarvamSpeechProvider:
    def __init__(
        self,
        api_key: str,
        model: str,
        voices: dict[str, str],
        base_url: str = "https://api.sarvam.ai",
        timeout: float = 60.0,
        transport: httpx.BaseTransport | None = None,
    ) -> None:
        self._url = base_url.rstrip("/") + "/text-to-speech"
        self._headers = {"api-subscription-key": api_key}
        self.model = model
        self._voices = voices
        self._client = httpx.Client(
            timeout=timeout,
            transport=transport,
            verify=_ssl_context() if transport is None else True,
        )

    def voice_for(self, language: Language) -> str:
        return self._voices.get(language, "")

    def synthesize(
        self, text: str, language: Language, *, voice: str = "default", sample_rate_hz: int = 22050
    ) -> SynthesisResult:
        speaker = self.voice_for(language) if voice == "default" else voice
        body: dict[str, Any] = {
            "text": text,
            "language_code": LANGUAGE_CODES[language],
            "model": self.model,
            "output_audio_codec": "mp3",
            "speech_sample_rate": sample_rate_hz,
        }
        if speaker:
            body["speaker"] = speaker
        try:
            r = self._client.post(self._url, json=body, headers=self._headers)
        except httpx.HTTPError as exc:
            raise SpeechError(f"network error: {exc}") from exc
        if r.status_code >= 400:
            raise SpeechError(f"HTTP {r.status_code}: {r.text[:200]}")
        try:
            audio = base64.b64decode(r.json()["audios"][0])
        except (KeyError, IndexError, ValueError, binascii.Error) as exc:
            raise SpeechError(f"unexpected reply: {exc}") from exc
        return SynthesisResult(audio=audio, sample_rate_hz=sample_rate_hz, media_type="audio/mpeg")

    def transcribe(self, audio: bytes, language: Language, *, sample_rate_hz: int) -> Transcript:
        raise NotImplementedError("speech-to-text arrives in M7")
