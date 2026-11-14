"""users table with row-level security

Revision ID: 0001_users
Revises:
Create Date: 2026-11-14
"""

from collections.abc import Sequence

import sqlalchemy as sa

from alembic import op

revision: str = "0001_users"
down_revision: str | None = None
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

# On Supabase the `auth` schema, `auth.users`, `auth.uid()` and the `authenticated` role
# already exist. On plain local Postgres (docker-compose, CI) we create minimal stand-ins
# so the same migration and policies apply. See docs/decisions.md.
LOCAL_AUTH_STUBS = """
CREATE SCHEMA IF NOT EXISTS auth;
CREATE TABLE IF NOT EXISTS auth.users (id uuid PRIMARY KEY);
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'authenticated') THEN
    CREATE ROLE authenticated NOLOGIN;
  END IF;
END
$$;
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname = 'auth' AND p.proname = 'uid'
  ) THEN
    EXECUTE $f$
      CREATE FUNCTION auth.uid() RETURNS uuid LANGUAGE sql STABLE AS
      'SELECT nullif(current_setting(''request.jwt.claim.sub'', true), '''')::uuid'
    $f$;
  END IF;
END
$$;
GRANT USAGE ON SCHEMA auth TO authenticated;
"""

CREATE_USERS = """
CREATE TABLE public.users (
  id uuid PRIMARY KEY REFERENCES auth.users (id) ON DELETE CASCADE,
  name text NOT NULL DEFAULT '',
  phone varchar(20) NOT NULL UNIQUE,
  roles text[] NOT NULL DEFAULT '{}'::text[],
  language_mode text NOT NULL DEFAULT 'hindi',
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT users_roles_valid CHECK (roles <@ ARRAY['student','teacher','parent']::text[]),
  CONSTRAINT users_language_mode_valid CHECK (language_mode IN ('hindi','kannada'))
);
"""

# Kept in sync with supabase/policies/users.sql (a test checks they match).
USERS_POLICIES = """
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_select_own ON public.users
  FOR SELECT TO authenticated
  USING (auth.uid() = id);

CREATE POLICY users_update_own ON public.users
  FOR UPDATE TO authenticated
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

GRANT SELECT, UPDATE ON public.users TO authenticated;
"""


def upgrade() -> None:
    is_supabase = op.get_bind().execute(sa.text("SELECT to_regclass('auth.users')")).scalar()
    if is_supabase is None:
        op.execute(LOCAL_AUTH_STUBS)
    op.execute(CREATE_USERS)
    op.execute(USERS_POLICIES)


def downgrade() -> None:
    op.execute("DROP TABLE IF EXISTS public.users")
