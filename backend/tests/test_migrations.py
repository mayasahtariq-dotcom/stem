"""Migration checks. Database tests need Postgres (see conftest.database_url); the
policy-sync checks always run."""

import importlib.util
import re
import uuid
from pathlib import Path
from types import ModuleType

import pytest
from sqlalchemy import create_engine, text

from tests.conftest import run_migrations

BACKEND = Path(__file__).resolve().parents[1]
REPO = BACKEND.parent


def _load(name: str) -> ModuleType:
    path = BACKEND / "alembic" / "versions" / f"{name}.py"
    spec = importlib.util.spec_from_file_location(name, path)
    assert spec and spec.loader
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _sql_statements(sql: str) -> list[str]:
    sql = re.sub(r"--[^\n]*", "", sql)
    return [" ".join(s.split()) for s in sql.split(";") if s.strip()]


@pytest.mark.parametrize(
    "migration,attr,policy_file",
    [
        ("0001_users", "USERS_POLICIES", "users.sql"),
        ("0002_classrooms", "CLASSROOM_POLICIES", "classrooms.sql"),
        ("0003_courses", "COURSE_POLICIES", "courses.sql"),
        ("0004_learning", "LEARNING_POLICIES", "learning.sql"),
        ("0005_parents_twin", "PARENT_POLICIES", "parents.sql"),
    ],
)
def test_policy_file_matches_migration(migration: str, attr: str, policy_file: str) -> None:
    sql = (REPO / "supabase" / "policies" / policy_file).read_text(encoding="utf-8")
    assert _sql_statements(sql) == _sql_statements(getattr(_load(migration), attr))


def test_migrations_upgrade_downgrade_and_users_rls(database_url: str) -> None:
    run_migrations(database_url)  # fresh: down to base, up to head
    run_migrations(database_url, target="0001_users")  # downgrade path works
    run_migrations(database_url, fresh=False)  # and back up

    me, other = uuid.uuid4(), uuid.uuid4()
    engine = create_engine(database_url)
    with engine.begin() as conn:
        conn.execute(text("TRUNCATE users, auth.users CASCADE"))
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
        conn.execute(text("TRUNCATE users, auth.users CASCADE"))
    engine.dispose()
