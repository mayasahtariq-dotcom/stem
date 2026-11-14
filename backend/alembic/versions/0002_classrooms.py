"""classrooms, memberships, join attempts, with row-level security

Revision ID: 0002_classrooms
Revises: 0001_users
Create Date: 2026-11-14
"""

from collections.abc import Sequence

from alembic import op

revision: str = "0002_classrooms"
down_revision: str | None = "0001_users"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

CREATE_TABLES = """
CREATE TABLE public.classrooms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  teacher_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  name text NOT NULL CHECK (char_length(name) BETWEEN 1 AND 60),
  subject text NOT NULL CHECK (subject IN ('BI','CH','PH')),
  grade smallint NOT NULL CHECK (grade BETWEEN 6 AND 12),
  join_key text NOT NULL UNIQUE,
  expires_at timestamptz,
  allowed_language_modes text[] NOT NULL DEFAULT ARRAY['hindi','kannada']::text[],
  created_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT classrooms_modes_valid CHECK (
    cardinality(allowed_language_modes) > 0
    AND allowed_language_modes <@ ARRAY['hindi','kannada']::text[]
  )
);
CREATE INDEX classrooms_teacher_idx ON public.classrooms (teacher_id);

CREATE TABLE public.memberships (
  classroom_id uuid NOT NULL REFERENCES public.classrooms (id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  joined_at timestamptz NOT NULL DEFAULT now(),
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','removed')),
  PRIMARY KEY (classroom_id, student_id)
);
CREATE INDEX memberships_student_idx ON public.memberships (student_id);

CREATE TABLE public.join_attempts (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  join_key text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX join_attempts_user_time_idx ON public.join_attempts (user_id, created_at);
"""

# SECURITY DEFINER helpers read across tables without triggering each other's policies
# (policies that query RLS-protected tables directly would recurse).
HELPERS = """
CREATE SCHEMA IF NOT EXISTS app_private;
GRANT USAGE ON SCHEMA app_private TO authenticated;

CREATE OR REPLACE FUNCTION app_private.is_member(cid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM memberships
                 WHERE classroom_id = cid AND student_id = auth.uid() AND status = 'active')
$$;

CREATE OR REPLACE FUNCTION app_private.teaches(cid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM classrooms WHERE id = cid AND teacher_id = auth.uid())
$$;

CREATE OR REPLACE FUNCTION app_private.teaches_student(sid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM memberships m JOIN classrooms c ON c.id = m.classroom_id
                 WHERE m.student_id = sid AND c.teacher_id = auth.uid() AND m.status = 'active')
$$;

CREATE OR REPLACE FUNCTION app_private.is_my_teacher(tid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM memberships m JOIN classrooms c ON c.id = m.classroom_id
                 WHERE c.teacher_id = tid AND m.student_id = auth.uid() AND m.status = 'active')
$$;

-- Student count for a classroom, only for its teacher or one of its students.
CREATE OR REPLACE FUNCTION app_private.member_count(cid uuid) RETURNS integer
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT CASE WHEN app_private.teaches(cid) OR app_private.is_member(cid)
    THEN (SELECT count(*)::int FROM memberships WHERE classroom_id = cid AND status = 'active')
  END
$$;

-- Whether a join key is in use by any classroom (keys must be unique across all teachers).
CREATE OR REPLACE FUNCTION app_private.join_key_taken(k text) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM classrooms WHERE join_key = k)
$$;

REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app_private FROM PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app_private TO authenticated;
"""

# Kept in sync with supabase/policies/classrooms.sql (a test checks they match).
CLASSROOM_POLICIES = """
ALTER TABLE public.classrooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.memberships ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.join_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY classrooms_select ON public.classrooms
  FOR SELECT TO authenticated
  USING (teacher_id = auth.uid() OR app_private.is_member(id));

CREATE POLICY classrooms_insert ON public.classrooms
  FOR INSERT TO authenticated
  WITH CHECK (teacher_id = auth.uid());

CREATE POLICY classrooms_update ON public.classrooms
  FOR UPDATE TO authenticated
  USING (teacher_id = auth.uid())
  WITH CHECK (teacher_id = auth.uid());

CREATE POLICY memberships_select ON public.memberships
  FOR SELECT TO authenticated
  USING (student_id = auth.uid() OR app_private.teaches(classroom_id));

CREATE POLICY memberships_insert ON public.memberships
  FOR INSERT TO authenticated
  WITH CHECK (student_id = auth.uid());

CREATE POLICY join_attempts_select ON public.join_attempts
  FOR SELECT TO authenticated
  USING (user_id = auth.uid());

CREATE POLICY join_attempts_insert ON public.join_attempts
  FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

CREATE POLICY users_insert_own ON public.users
  FOR INSERT TO authenticated
  WITH CHECK (auth.uid() = id);

CREATE POLICY users_select_related ON public.users
  FOR SELECT TO authenticated
  USING (app_private.teaches_student(id) OR app_private.is_my_teacher(id));

GRANT SELECT, INSERT, UPDATE ON public.classrooms TO authenticated;
GRANT SELECT, INSERT ON public.memberships TO authenticated;
GRANT SELECT, INSERT ON public.join_attempts TO authenticated;
GRANT INSERT ON public.users TO authenticated;
"""


def upgrade() -> None:
    op.execute(CREATE_TABLES)
    op.execute(HELPERS)
    op.execute(CLASSROOM_POLICIES)


def downgrade() -> None:
    op.execute("DROP POLICY IF EXISTS users_select_related ON public.users")
    op.execute("DROP POLICY IF EXISTS users_insert_own ON public.users")
    op.execute("REVOKE INSERT ON public.users FROM authenticated")
    op.execute("DROP TABLE IF EXISTS public.join_attempts")
    op.execute("DROP TABLE IF EXISTS public.memberships")
    op.execute("DROP TABLE IF EXISTS public.classrooms")
    op.execute("DROP SCHEMA IF EXISTS app_private CASCADE")
