from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Settings loaded from the environment or a `.env` file. Never commit `.env`."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "postgresql+psycopg://postgres:postgres@localhost:5432/stemtutor"
    supabase_url: str = ""
    supabase_jwt_secret: str = ""
    supabase_service_key: str = ""
    anthropic_api_key: str = ""
    sarvam_api_key: str = ""
    log_level: str = "INFO"

    # ---- AI (M2). Open-source models; fakes until configured. --------------------------
    # true: fake OCR, LLM and embeddings (tests, development without models).
    use_fake_ai: bool = True
    # Any OpenAI-compatible chat API: OpenRouter, Together, Groq, vLLM, Ollama...
    llm_base_url: str = "https://openrouter.ai/api/v1"
    llm_api_key: str = ""
    # Model names are settings, never hard-coded (PRD). Chosen by the bake-off on real notes.
    # Each may be a comma-separated list, tried in order when a model is busy.
    llm_outline_model: str = "nvidia/nemotron-3-ultra-550b-a55b:free,google/gemma-4-31b-it:free"
    llm_terms_model: str = (
        "nvidia/nemotron-3.5-super-vl:free,nvidia/nemotron-3-ultra-550b-a55b:free"
    )
    llm_questions_model: str = "nvidia/nemotron-3-ultra-550b-a55b:free,google/gemma-4-31b-it:free"
    llm_timeout_seconds: float = 120.0
    # Rough cost estimate per million tokens, in US dollars, for the generation metrics.
    llm_price_input_per_mtok: float = 0.0
    llm_price_output_per_mtok: float = 0.0
    embedding_model: str = "BAAI/bge-m3"
    embedding_dim: int = 1024

    # ---- Storage and jobs ------------------------------------------------------------------
    storage_backend: Literal["local", "supabase"] = "local"
    storage_bucket: str = "notes"
    local_storage_dir: str = "var/uploads"
    # Base URL the app uses to reach this API (for LocalStorage upload links).
    public_base_url: str = "http://localhost:8000"
    # true: run background jobs in-process (tests, development). false: procrastinate queue.
    jobs_inline: bool = True


@lru_cache
def get_settings() -> Settings:
    return Settings()
