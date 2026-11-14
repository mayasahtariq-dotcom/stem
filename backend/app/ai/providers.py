"""Builds the AI, storage and job providers from settings. Overridable in tests via
`app.dependency_overrides[get_services]`."""

from collections.abc import Callable
from dataclasses import dataclass
from functools import lru_cache
from typing import Annotated, Any

from fastapi import Depends

from app.ai.embeddings import EmbeddingProvider, FakeEmbeddingProvider
from app.ai.llm import FakeLLMProvider, LLMProvider, OpenAICompatibleLLMProvider
from app.ai.ocr import FakeOcrProvider, OcrProvider
from app.config import Settings, get_settings
from app.storage import LocalStorage, StorageProvider, SupabaseStorage


class JobRunner:
    """Runs background jobs. Inline (in-process, right away) for tests and development; the
    procrastinate queue in production (see app/jobs/queue.py)."""

    def __init__(self, inline: bool = True) -> None:
        self.inline = inline
        self.enqueued: list[tuple[str, dict[str, Any]]] = []
        self._handlers: dict[str, Callable[..., None]] = {}

    def register(self, name: str, handler: Callable[..., None]) -> None:
        self._handlers[name] = handler

    def enqueue(self, name: str, **kwargs: Any) -> None:
        self.enqueued.append((name, kwargs))
        if self.inline:
            self._handlers[name](**kwargs)
        else:  # pragma: no cover - production path
            from app.jobs.queue import defer

            defer(name, **kwargs)


@dataclass
class Services:
    settings: Settings
    ocr: OcrProvider
    llm: LLMProvider
    embeddings: EmbeddingProvider
    storage: StorageProvider
    jobs: JobRunner


def build_services(settings: Settings) -> Services:
    if settings.use_fake_ai:
        ocr: OcrProvider = FakeOcrProvider()
        llm: LLMProvider = FakeLLMProvider(responder=_fake_responder())
        embeddings: EmbeddingProvider = FakeEmbeddingProvider(settings.embedding_dim)
    else:  # pragma: no cover - needs real models
        from app.ai.embeddings import SentenceTransformerEmbeddingProvider
        from app.ai.ocr import SuryaOcrProvider

        ocr = SuryaOcrProvider()
        llm = OpenAICompatibleLLMProvider(
            settings.llm_base_url, settings.llm_api_key, settings.llm_timeout_seconds
        )
        embeddings = SentenceTransformerEmbeddingProvider(
            settings.embedding_model, settings.embedding_dim
        )
    storage: StorageProvider
    if settings.storage_backend == "supabase":  # pragma: no cover - needs Supabase
        storage = SupabaseStorage(
            settings.supabase_url, settings.supabase_service_key, settings.storage_bucket
        )
    else:
        storage = LocalStorage(
            settings.local_storage_dir, settings.public_base_url, settings.supabase_jwt_secret
        )
    services = Services(
        settings, ocr, llm, embeddings, storage, JobRunner(inline=settings.jobs_inline)
    )
    from app.ai.pipeline import register_jobs

    register_jobs(services)
    return services


def _fake_responder() -> Callable[[Any], str]:
    from app.ai.fake_content import fake_llm_response

    return fake_llm_response


@lru_cache(maxsize=1)
def _cached_services() -> Services:
    return build_services(get_settings())


def get_services() -> Services:
    return _cached_services()


ServicesDep = Annotated[Services, Depends(get_services)]
