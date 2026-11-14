"""Migration checks. The database test runs only when TEST_DATABASE_URL points at Postgres
(CI provides one as a service container); the policy-sync check always runs."""

import importlib.util
import os
import re
import uuid
from pathlib import Path
from types import ModuleType

import pytest
from sqlalchemy import create_engine, text

BACKEND = Path(__file__).resolve().parents[1]
REPO = BACKEND.parent


def _load_migration() -> ModuleType:
    path = BACKEND / "alembic" / "versions" / "0001_users.py"
    spec = importlib.util.spec_from_file_location("m0001", path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _sql_statements(sql: str) -> list[str]:
    sql = re.sub(r"--[^\n]*", "", sql)
    return [" ".join(s.split()) for s in sql.split(";") if s.strip()]


def test_policy_file_matches_migration() -> None:
    migration = _load_migration()
    policy_file = (REPO / "supabase" / "policies" / "users.sql").read_text(encoding="utf-8")
    assert _sql_statements(policy_file) == _sql_statements(migration.USERS_POLICIES)


DB_URL = os.environ.get("TEST_DATABASE_URL")


@pytest.mark.skipif(not DB_URL, reason="TEST_DATABASE_URL not set")
def test_users_table_and_rls_on_postgres(monkeypatch: pytest.MonkeyPatch) -> None:
    from alembic.config import Config

    from alembic import command

    assert DB_URL is not None
    monkeypatch.setenv("DATABASE_URL", DB_URL)
    from app.config import get_settings

    get_settings.cache_clear()
    cfg = Config(str(BACKEND / "alembic.ini"))
    cfg.set_main_option("script_location", str(BACKEND / "alembic"))
    command.downgrade(cfg, "base")
    command.upgrade(cfg, "head")

    me, other = uuid.uuid4(), uuid.uuid4()
    engine = create_engine(DB_URL)
    with engine.begin() as conn:
        for uid, phone in ((me, "+919000000001"), (other, "+919000000002")):
            conn.execute(text("INSERT INTO auth.users (id) VALUES (:id)"), {"id": uid})
            conn.execute(
                text("INSERT INTO users (id, phone, roles) VALUES (:id, :p, ARRAY['student'])"),
                {"id": uid, "p": phone},
            )
        row = conn.execute(text("SELECT language_mode FROM users WHERE id = :id"), {"id": me})
        assert row.scalar_one() == "hindi"

    with engine.begin() as conn:
        conn.execute(text("SET LOCAL ROLE authenticated"))
        conn.execute(text("SELECT set_config('request.jwt.claim.sub', :s, true)"), {"s": str(me)})
        visible = conn.execute(text("SELECT id FROM users")).scalars().all()
        assert visible == [me]
        updated = conn.execute(text("UPDATE users SET name = 'x' WHERE id = :id"), {"id": other})
        assert updated.rowcount == 0
        updated = conn.execute(text("UPDATE users SET name = 'me' WHERE id = :id"), {"id": me})
        assert updated.rowcount == 1

    with engine.begin() as conn:
        conn.execute(text("DELETE FROM users"))
        conn.execute(text("DELETE FROM auth.users"))
    get_settings.cache_clear()
