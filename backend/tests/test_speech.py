"""Sarvam text-to-speech client (with a mock transport) and the fake provider."""

import base64
import json

import httpx

from app.speech.provider import FakeSpeechProvider
from app.speech.sarvam import SarvamSpeechProvider, SpeechError


def test_sarvam_request_shape_and_decoding() -> None:
    seen: dict[str, object] = {}

    def handler(request: httpx.Request) -> httpx.Response:
        seen["url"] = str(request.url)
        seen["key"] = request.headers.get("api-subscription-key")
        seen["body"] = json.loads(request.content)
        return httpx.Response(200, json={"audios": [base64.b64encode(b"ID3mp3").decode()]})

    tts = SarvamSpeechProvider(
        "k-123",
        "bulbul:v4-flash",
        {"hi": "ritu_hi_edtech", "kn": "chaitra_kn_narration", "en": "ritu_en_edtech"},
        transport=httpx.MockTransport(handler),
    )
    result = tts.synthesize("नमस्ते", "hi")
    assert result.audio == b"ID3mp3" and result.media_type == "audio/mpeg"
    assert seen["url"] == "https://api.sarvam.ai/text-to-speech"
    assert seen["key"] == "k-123"
    body = seen["body"]
    assert isinstance(body, dict)
    assert body["language_code"] == "hi-IN" and body["speaker"] == "ritu_hi_edtech"
    assert body["model"] == "bulbul:v4-flash" and body["output_audio_codec"] == "mp3"


def test_sarvam_errors_are_reported() -> None:
    tts = SarvamSpeechProvider(
        "bad",
        "bulbul:v4-flash",
        {},
        transport=httpx.MockTransport(lambda r: httpx.Response(403, text="invalid key")),
    )
    try:
        tts.synthesize("hello", "en")
    except SpeechError as exc:
        assert "403" in str(exc)
    else:  # pragma: no cover
        raise AssertionError("expected SpeechError")


def test_fake_speech_returns_a_playable_wav() -> None:
    result = FakeSpeechProvider().synthesize("hi", "en")
    assert result.audio[:4] == b"RIFF" and result.audio[8:12] == b"WAVE"
    assert result.media_type == "audio/wav"
