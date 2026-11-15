"""Listen (PRD M3): an audio link for any text in a language.

Audio is made with the speech provider (Sarvam) and kept in storage under a hash of the
language, model, voice and text, so the same text is only ever synthesised once. With
TTS_PREGENERATE on, publishing a lesson makes the audio for all its questions, options and
terms up front; otherwise each clip is made the first time someone taps Listen.
"""

import hashlib
import logging
import uuid
from typing import Any

from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert

from app.ai.providers import Services
from app.db.models import AudioCache, Lesson, QuestionRow, TermRow
from app.speech.provider import Language

log = logging.getLogger(__name__)

EXTENSIONS = {"audio/mpeg": "mp3", "audio/wav": "wav"}


def audio_hash(language: str, model: str, voice: str, text: str) -> str:
    return hashlib.sha256(f"{language}|{model}|{voice}|{text}".encode()).hexdigest()


def audio_url(services: Services, text: str, language: Language) -> str:
    """Returns a link to the audio for [text], making and storing it on first use."""
    from app.ai.pipeline import _session

    speech = services.speech
    voice = speech.voice_for(language)
    key = audio_hash(language, speech.model, voice, text.strip())
    with _session(services) as s:
        cached = s.get(AudioCache, key)
        if cached is not None and services.storage.exists(cached.storage_path):
            return services.storage.download_url(cached.storage_path)
    result = speech.synthesize(text.strip(), language)
    path = f"audio/{key}.{EXTENSIONS.get(result.media_type, 'bin')}"
    services.storage.write(path, result.audio, result.media_type)
    with _session(services) as s, s.begin():
        s.execute(
            insert(AudioCache)
            .values(hash=key, language=language, voice=voice, model=speech.model, storage_path=path)
            .on_conflict_do_update(index_elements=[AudioCache.hash], set_={"storage_path": path})
        )
    return services.storage.download_url(path)


def _texts(value: Any) -> list[tuple[str, Language]]:
    """Every {en, hi, kn} text inside a question's content or a term card."""
    out: list[tuple[str, Language]] = []
    if isinstance(value, dict):
        if {"en", "hi", "kn"} <= value.keys() and all(
            isinstance(value[k], str) for k in ("en", "hi", "kn")
        ):
            for lang in ("hi", "kn", "en"):
                if value[lang].strip():
                    out.append((value[lang], lang))
        else:
            for k, v in value.items():
                if k not in ("call_script",):
                    out.extend(_texts(v))
    elif isinstance(value, list):
        for v in value:
            out.extend(_texts(v))
    return out


def lesson_texts(services: Services, lesson_id: uuid.UUID) -> list[tuple[str, Language]]:
    from app.ai.pipeline import _session

    with _session(services) as s:
        lesson = s.get(Lesson, lesson_id)
        if lesson is None:
            return []
        texts: list[tuple[str, Language]] = []
        for q in s.scalars(select(QuestionRow).where(QuestionRow.lesson_id == lesson_id)):
            texts.extend(_texts(q.content))
        for t in s.scalars(select(TermRow).where(TermRow.course_id == lesson.course_id)):
            texts.extend(
                _texts({"forms": t.content.get("forms"), "meaning": t.content.get("meaning")})
            )
    return list(dict.fromkeys(texts))


def pregenerate_audio(services: Services, lesson_id: str) -> None:
    made = 0
    for text_, lang in lesson_texts(services, uuid.UUID(lesson_id)):
        try:
            audio_url(services, text_, lang)
            made += 1
        except Exception:  # noqa: BLE001 - one bad clip must not stop the rest
            log.warning("audio generation failed", extra={"lesson_id": lesson_id, "lang": lang})
    log.info("lesson audio ready", extra={"lesson_id": lesson_id, "clips": made})


def register_audio_jobs(services: Services) -> None:
    services.jobs.register(
        "pregenerate_audio", lambda lesson_id: pregenerate_audio(services, lesson_id)
    )
