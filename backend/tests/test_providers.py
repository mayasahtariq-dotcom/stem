from app.ai.llm import FakeLLMProvider, LLMProvider, LLMRequest
from app.speech.provider import FakeSpeechProvider, SpeechProvider


def test_fake_llm_returns_canned_responses_in_order() -> None:
    provider: LLMProvider = FakeLLMProvider(responses=['{"a": 1}', '{"b": 2}'])
    request = LLMRequest(model="fake", system="s", prompt="p")
    assert provider.complete(request).text == '{"a": 1}'
    assert provider.complete(request).text == '{"b": 2}'
    assert len(provider.embed(["x", "y"])) == 2


def test_fake_speech_is_deterministic() -> None:
    provider: SpeechProvider = FakeSpeechProvider(transcript="न्यूटन")
    result = provider.synthesize("नमस्ते", "hi", sample_rate_hz=8000)
    assert result.sample_rate_hz == 8000
    assert len(result.audio) == 1600
    assert provider.transcribe(b"", "hi", sample_rate_hz=16000).text == "न्यूटन"
