import os
import sys
import tempfile
import time
import uuid
from collections.abc import Iterator
from pathlib import Path
from typing import Any

import jwt
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import Engine, create_engine, text

from app.ai.providers import Services, build_services, get_services
from app.config import Settings, get_settings
from app.main import create_app

TEST_SECRET = "test-secret-at-least-32-bytes-long-000000"
TEST_USER_ID = "8f14e45f-ceea-467a-9575-6a1f3c2b7d10"
BACKEND = Path(__file__).resolve().parents[1]


def make_token(secret: str = TEST_SECRET, **overrides: Any) -> str:
    claims: dict[str, Any] = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "role": "authenticated",
        "exp": int(time.time()) + 3600,
    }
    claims.update(overrides)
    return jwt.encode(claims, secret, algorithm="HS256")


@pytest.fixture
def settings() -> Settings:
    return Settings(supabase_jwt_secret=TEST_SECRET, supabase_url="", _env_file=None)


@pytest.fixture
def client(settings: Settings) -> Iterator[TestClient]:
    app = create_app()
    app.dependency_overrides[get_settings] = lambda: settings
    with TestClient(app) as c:
        yield c


# ---- Database --------------------------------------------------------------------------
# CI sets TEST_DATABASE_URL (a Postgres service). Locally on Windows, an embedded Postgres
# (pgserver) is started instead. Without either, database tests are skipped.


def _local_pgserver_url() -> str | None:
    if sys.platform != "win32":
        return None
    try:
        import pgserver
    except ImportError:
        return None
    datadir = Path(tempfile.gettempdir()) / "stemtutor_pg_test"
    get_server = getattr(pgserver, "get_server")  # noqa: B009 - untyped, Windows-only package
    server = get_server(str(datadir), cleanup_mode="stop")
    uri: str = server.get_uri()
    return uri.replace("postgresql://", "postgresql+psycopg://", 1)


@pytest.fixture(scope="session")
def database_url() -> str:
    url = os.environ.get("TEST_DATABASE_URL") or _local_pgserver_url()
    if not url:
        pytest.skip("no test database (set TEST_DATABASE_URL)")
    return url


def run_migrations(url: str, target: str = "head", fresh: bool = True) -> None:
    from alembic.config import Config

    from alembic import command

    previous = os.environ.get("DATABASE_URL")
    os.environ["DATABASE_URL"] = url
    get_settings.cache_clear()
    try:
        cfg = Config(str(BACKEND / "alembic.ini"))
        cfg.set_main_option("script_location", str(BACKEND / "alembic"))
        if fresh:
            command.downgrade(cfg, "base")
        command.upgrade(cfg, target)
    finally:
        if previous is None:
            os.environ.pop("DATABASE_URL", None)
        else:
            os.environ["DATABASE_URL"] = previous
        get_settings.cache_clear()


@pytest.fixture(scope="session")
def migrated_engine(database_url: str) -> Iterator[Engine]:
    run_migrations(database_url)
    engine = create_engine(database_url)
    yield engine
    engine.dispose()


def wipe(engine: Engine) -> None:
    with engine.begin() as conn:
        conn.execute(
            text(
                "TRUNCATE twin_confusions, twin_concepts, deletion_requests, consent_records,"
                " link_codes, parent_links, audio_cache, doubts, saved_terms, student_stats,"
                " lesson_progress, answer_attempts, course_assignments, terms, questions, lessons,"
                " units, courses,"
                " note_chunks, upload_pages, uploads, join_attempts, memberships, classrooms,"
                " users, auth.users CASCADE"
            )
        )


@pytest.fixture
def db_settings(database_url: str, migrated_engine: Engine) -> Settings:
    wipe(migrated_engine)
    return Settings(
        supabase_jwt_secret=TEST_SECRET,
        supabase_url="",
        database_url=database_url,
        _env_file=None,
    )


@pytest.fixture
def services(db_settings: Settings, tmp_path: Path) -> Services:
    """Fake AI, files in a temp folder, jobs run inline."""
    settings = db_settings.model_copy(
        update={
            "use_fake_ai": True,
            "storage_backend": "local",
            "local_storage_dir": str(tmp_path / "uploads"),
            "public_base_url": "http://testserver",
            "jobs_inline": True,
            "dashboard_cache_seconds": 0,
        }
    )
    return build_services(settings)


@pytest.fixture
def api(db_settings: Settings, services: Services) -> Iterator[TestClient]:
    app = create_app()
    app.dependency_overrides[get_settings] = lambda: db_settings
    app.dependency_overrides[get_services] = lambda: services
    with TestClient(app) as c:
        yield c


class Person:
    """A signed-in test user: an auth.users row plus a token carrying their phone."""

    def __init__(self, api: TestClient, engine: Engine, phone: str) -> None:
        self.id = str(uuid.uuid4())
        self.phone = phone
        self.api = api
        with engine.begin() as conn:
            conn.execute(text("INSERT INTO auth.users (id) VALUES (:id)"), {"id": self.id})
        self.headers = {
            "Authorization": f"Bearer {make_token(sub=self.id, phone=phone.lstrip('+'))}"
        }

    def setup(self, name: str, roles: list[str], language_mode: str = "hindi") -> "Person":
        r = self.api.put(
            "/me",
            json={"name": name, "roles": roles, "language_mode": language_mode},
            headers=self.headers,
        )
        assert r.status_code == 200, r.text
        return self

    def get(self, path: str) -> Any:
        return self.api.get(path, headers=self.headers)

    def post(self, path: str, body: dict[str, Any]) -> Any:
        return self.api.post(path, json=body, headers=self.headers)

    def put(self, path: str, body: dict[str, Any]) -> Any:
        return self.api.put(path, json=body, headers=self.headers)

    def delete(self, path: str) -> Any:
        return self.api.delete(path, headers=self.headers)


@pytest.fixture
def people(api: TestClient, migrated_engine: Engine) -> Any:
    counter = iter(range(9_000_000_000, 9_999_999_999))

    def make(name: str, roles: list[str], language_mode: str = "hindi") -> Person:
        return Person(api, migrated_engine, f"+91{next(counter)}").setup(name, roles, language_mode)

    return make
