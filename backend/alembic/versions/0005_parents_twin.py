"""parent links, consent, deletion requests, school consent, and the Student Twin

Revision ID: 0005_parents_twin
Revises: 0004_learning
Create Date: 2026-11-15
"""

from collections.abc import Sequence

from alembic import op

revision: str = "0005_parents_twin"
down_revision: str | None = "0004_learning"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

CREATE_TABLES = """
ALTER TABLE public.users
  ADD COLUMN consent_status text NOT NULL DEFAULT 'pending'
    CHECK (consent_status IN ('pending','approved','revoked'));

ALTER TABLE public.classrooms
  ADD COLUMN school_consent boolean NOT NULL DEFAULT false,
  ADD COLUMN school_consent_by uuid REFERENCES public.users (id) ON DELETE SET NULL,
  ADD COLUMN school_consent_at timestamptz;

CREATE TABLE public.parent_links (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  parent_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  status text NOT NULL DEFAULT 'active' CHECK (status IN ('active','revoked')),
  linked_at timestamptz NOT NULL DEFAULT now(),
  consent_at timestamptz,
  revoked_at timestamptz,
  UNIQUE (parent_id, student_id)
);
CREATE INDEX parent_links_student_idx ON public.parent_links (student_id);

CREATE TABLE public.link_codes (
  code text PRIMARY KEY,
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  created_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  used_at timestamptz,
  used_by uuid REFERENCES public.users (id) ON DELETE SET NULL
);

CREATE TABLE public.consent_records (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  source text NOT NULL CHECK (source IN ('parent','school')),
  parent_id uuid REFERENCES public.users (id) ON DELETE SET NULL,
  classroom_id uuid REFERENCES public.classrooms (id) ON DELETE SET NULL,
  given_at timestamptz NOT NULL DEFAULT now(),
  revoked_at timestamptz
);
CREATE INDEX consent_records_student_idx ON public.consent_records (student_id);

CREATE TABLE public.deletion_requests (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL,
  requested_by uuid,
  requested_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz
);

CREATE TABLE public.twin_concepts (
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  kind text NOT NULL CHECK (kind IN ('lesson','term')),
  key text NOT NULL,
  mastery real NOT NULL,
  half_life_days real NOT NULL,
  attempts integer NOT NULL DEFAULT 0,
  correct integer NOT NULL DEFAULT 0,
  last_seen_at timestamptz,
  next_review_at timestamptz,
  PRIMARY KEY (student_id, course_id, kind, key)
);

CREATE TABLE public.twin_confusions (
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  expected_key text NOT NULL,
  chosen_key text NOT NULL,
  count integer NOT NULL DEFAULT 1,
  last_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (student_id, course_id, expected_key, chosen_key)
);
"""

HELPERS = """
-- The current user is an active (linked) parent of student [sid].
CREATE OR REPLACE FUNCTION app_private.is_parent_of(sid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM parent_links
                 WHERE student_id = sid AND parent_id = auth.uid() AND status = 'active')
$$;

-- [pid] is an active parent of the current user (a student sees their linked parents).
CREATE OR REPLACE FUNCTION app_private.is_my_parent(pid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM parent_links
                 WHERE parent_id = pid AND student_id = auth.uid() AND status = 'active')
$$;

REVOKE ALL ON FUNCTION app_private.is_parent_of(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION app_private.is_my_parent(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app_private.is_parent_of(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION app_private.is_my_parent(uuid) TO authenticated;
"""

# Kept in sync with supabase/policies/parents.sql (a test checks they match).
PARENT_POLICIES = """
ALTER TABLE public.parent_links ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.link_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.consent_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.deletion_requests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.twin_concepts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.twin_confusions ENABLE ROW LEVEL SECURITY;

CREATE POLICY parent_links_select ON public.parent_links
  FOR SELECT TO authenticated
  USING (parent_id = auth.uid() OR student_id = auth.uid());

CREATE POLICY link_codes_select ON public.link_codes
  FOR SELECT TO authenticated
  USING (student_id = auth.uid());

CREATE POLICY consent_records_select ON public.consent_records
  FOR SELECT TO authenticated
  USING (student_id = auth.uid() OR app_private.is_parent_of(student_id));

CREATE POLICY deletion_requests_select ON public.deletion_requests
  FOR SELECT TO authenticated
  USING (student_id = auth.uid() OR requested_by = auth.uid());

CREATE POLICY twin_concepts_select ON public.twin_concepts
  FOR SELECT TO authenticated
  USING (student_id = auth.uid() OR app_private.teaches_student(student_id)
         OR app_private.is_parent_of(student_id));

CREATE POLICY twin_confusions_select ON public.twin_confusions
  FOR SELECT TO authenticated
  USING (student_id = auth.uid() OR app_private.teaches_student(student_id)
         OR app_private.is_parent_of(student_id));

CREATE POLICY users_select_family ON public.users
  FOR SELECT TO authenticated
  USING (app_private.is_parent_of(id) OR app_private.is_my_parent(id));

CREATE POLICY stats_teacher ON public.student_stats
  FOR SELECT TO authenticated
  USING (app_private.teaches_student(student_id));

CREATE POLICY stats_parent ON public.student_stats
  FOR SELECT TO authenticated
  USING (app_private.is_parent_of(student_id));

CREATE POLICY progress_parent ON public.lesson_progress
  FOR SELECT TO authenticated
  USING (app_private.is_parent_of(student_id));

CREATE POLICY attempts_parent ON public.answer_attempts
  FOR SELECT TO authenticated
  USING (app_private.is_parent_of(student_id));

GRANT SELECT ON public.parent_links, public.link_codes, public.consent_records,
  public.deletion_requests, public.twin_concepts, public.twin_confusions TO authenticated;
"""


def upgrade() -> None:
    op.execute(CREATE_TABLES)
    op.execute(HELPERS)
    op.execute(PARENT_POLICIES)


def downgrade() -> None:
    for policy, table in (
        ("users_select_family", "users"),
        ("stats_teacher", "student_stats"),
        ("stats_parent", "student_stats"),
        ("progress_parent", "lesson_progress"),
        ("attempts_parent", "answer_attempts"),
    ):
        op.execute(f"DROP POLICY IF EXISTS {policy} ON public.{table}")
    for table in (
        "twin_confusions",
        "twin_concepts",
        "deletion_requests",
        "consent_records",
        "link_codes",
        "parent_links",
    ):
        op.execute(f"DROP TABLE IF EXISTS public.{table}")
    op.execute("DROP FUNCTION IF EXISTS app_private.is_parent_of(uuid)")
    op.execute("DROP FUNCTION IF EXISTS app_private.is_my_parent(uuid)")
    op.execute(
        "ALTER TABLE public.classrooms DROP COLUMN IF EXISTS school_consent,"
        " DROP COLUMN IF EXISTS school_consent_by, DROP COLUMN IF EXISTS school_consent_at"
    )
    op.execute("ALTER TABLE public.users DROP COLUMN IF EXISTS consent_status")
