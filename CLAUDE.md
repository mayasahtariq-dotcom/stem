# STEM Tutor

A Duolingo-style bilingual (Hindi/Kannada + English) learning app for Biology, Chemistry and Physics. The full spec is `docs/PRD.md`: read its overview before any milestone, build milestones strictly in order, and don't build features from later milestones early. Wherever the PRD says VERIFY, read the vendor's current docs before writing code.

## Rules (from the PRD)

- Never commit secrets. Keys come from `local.properties` for the app and `.env` for the backend, with example files committed instead.
- Every external service (Claude, Sarvam, Exotel, Firebase) sits behind an interface with a fake version used in tests and debug builds.
- Every screen has its own state class, a ViewModel, a stateless screen composable and previews in both Hindi and Kannada.
- Every screen handles loading, empty, error and success states, so the user never sees a blank screen. No business logic lives in UI code.
- A milestone isn't done while any test fails.
- After each milestone, tick its checklist in `docs/progress.md`, write a short demo note in `docs/demos/` saying what to tap to see it working, and log any deviation from this PRD in `docs/decisions.md`.
- If an instruction is unclear or conflicts with a vendor's docs, choose the simplest safe option, log it in `docs/decisions.md`, and continue.

## Language rules (short form)

- Every user-facing string exists in `values` (English), `values-hi` and `values-kn`. Course content is JSON with `en`, `hi` and `kn` fields.
- Use `BilingualText` for every question, option, button and explanation: bold regional line above the muted English line. Get both lines with `bilingualStringResource(R.string.x)`.
- The app language follows the stored language mode (`LanguageRepository`), never the phone's language.
- Every Hindi/Kannada string written is a draft: add it to `docs/translations-to-review.md`.
- Say "Not quite", never "Wrong". Text on baby blue or cream is always dark.

## Layout

- `app/`: Android Gradle root. `app/app` is the application module; `core/{designsystem,common,network,data}`; `feature/{onboarding,student,teacher,parent}`. Versions are in `app/gradle/libs.versions.toml`.
- `backend/`: FastAPI (Python 3.12). `app/routers`, `app/ai` (LLMProvider + prompts), `app/speech` (SpeechProvider), `app/calls`, `app/jobs`, `app/db`, `alembic/`, `tests/`.
- `supabase/policies/`: RLS SQL (kept in sync with the Alembic migrations; a test checks it).
- `docs/`: PRD, progress, decisions, translations to review, demo notes.

## Commands

Android (needs JDK 17 + Android SDK; CI runs exactly this):
```
cd app && ./gradlew lint testDebugUnitTest assembleDebug
```

Backend:
```
cd backend
python -m venv .venv && .venv/Scripts/pip install -e ".[dev]"   # bin/ instead of Scripts/ on Linux/macOS
ruff check . && ruff format --check . && mypy app tests && pytest
docker compose up --build        # Postgres (pgvector) + API on :8000
```
Set `TEST_DATABASE_URL` to run the migration/RLS test against a real Postgres.
