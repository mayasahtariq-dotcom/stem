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
    # Doubt chat answers: short, so a fast model is fine.
    llm_doubt_model: str = (
        "nvidia/nemotron-3.5-super-vl:free,nvidia/nemotron-3-ultra-550b-a55b:free"
    )
    llm_timeout_seconds: float = 300.0  # free models can take minutes for a lesson
    # Rough cost estimate per million tokens, in US dollars, for the generation metrics.
    llm_price_input_per_mtok: float = 0.0
    llm_price_output_per_mtok: float = 0.0
    # Surya's layout model finds diagrams on note photos; slow on a CPU, so off by default.
    ocr_detect_diagrams: bool = False
    embedding_model: str = "BAAI/bge-m3"
    fake_embeddings: bool = False
    embedding_dim: int = 1024

    # ---- Student learning (M3) ----------------------------------------------------------
    doubts_per_day: int = 20
    # Teacher dashboard numbers are cached per classroom (PRD M4: five minutes). 0 turns it off.
    dashboard_cache_seconds: int = 300

    # ---- Speech (M3 Listen). Sarvam text-to-speech; fake while SARVAM_API_KEY is blank. ----
    use_fake_speech: bool = False
    sarvam_base_url: str = "https://api.sarvam.ai"
    tts_model: str = "bulbul:v4-flash"
    # Voice per language (Sarvam bulbul:v4-flash speaker IDs from its Voices guide).
    tts_voice_hi: str = "ritu_hi_edtech"
    tts_voice_kn: str = "chaitra_kn_narration"
    tts_voice_en: str = "ritu_en_edtech"
    # true: make all of a lesson's audio when it's published (PRD). false (default while
    # testing on free credit): make each clip the first time someone taps Listen.
    tts_pregenerate: bool = False
    tts_max_chars: int = 2500
    audio_requests_per_hour: int = 300

    # ---- Storage and jobs ------------------------------------------------------------------
    storage_backend: Literal["local", "supabase"] = "local"
    storage_bucket: str = "notes"
    local_storage_dir: str = "var/uploads"
    # Base URL the app uses to reach this API (for LocalStorage upload links).
    public_base_url: str = "http://localhost:8000"
    # true: run background jobs in-process (tests, development). false: procrastinate queue.
    jobs_inline: bool = True
    # Local development against an embedded Postgres (scripts/dev_server.py): there is no real
    # Supabase auth.users table, so signed-in users are added to the local stand-in first.
    local_auth_stub: bool = False


@lru_cache
def get_settings() -> Settings:
    return Settings()
