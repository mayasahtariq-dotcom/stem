"""uploads, note chunks (pgvector), courses, units, lessons, questions, terms, assignments

Revision ID: 0003_courses
Revises: 0002_classrooms
Create Date: 2026-11-14
"""

from collections.abc import Sequence

from alembic import op

revision: str = "0003_courses"
down_revision: str | None = "0002_classrooms"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

EMBEDDING_DIM = 1024  # BGE-M3; keep in sync with Settings.embedding_dim

CREATE_TABLES = f"""
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE public.uploads (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  classroom_id uuid REFERENCES public.classrooms (id) ON DELETE SET NULL,
  is_personal boolean NOT NULL DEFAULT false,
  subject text CHECK (subject IN ('BI','CH','PH')),
  grade smallint CHECK (grade BETWEEN 6 AND 12),
  page_count smallint NOT NULL CHECK (page_count BETWEEN 1 AND 10),
  status text NOT NULL DEFAULT 'created',
  error text,
  started_at timestamptz,
  finished_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX uploads_owner_idx ON public.uploads (owner_id);

CREATE TABLE public.upload_pages (
  upload_id uuid NOT NULL REFERENCES public.uploads (id) ON DELETE CASCADE,
  page_index smallint NOT NULL,
  kind text NOT NULL CHECK (kind IN ('photo','pdf','text')),
  storage_path text,
  typed_text text,
  ocr_text text,
  ocr_confidence real,
  diagrams jsonb NOT NULL DEFAULT '[]'::jsonb,
  PRIMARY KEY (upload_id, page_index)
);

CREATE TABLE public.note_chunks (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  upload_id uuid NOT NULL REFERENCES public.uploads (id) ON DELETE CASCADE,
  page smallint NOT NULL,
  text text NOT NULL,
  confidence real NOT NULL DEFAULT 1,
  embedding vector({EMBEDDING_DIM})
);
CREATE INDEX note_chunks_upload_idx ON public.note_chunks (upload_id);

CREATE TABLE public.courses (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  upload_id uuid REFERENCES public.uploads (id) ON DELETE SET NULL,
  classroom_id uuid REFERENCES public.classrooms (id) ON DELETE SET NULL,
  subject text NOT NULL CHECK (subject IN ('BI','CH','PH')),
  grade smallint NOT NULL CHECK (grade BETWEEN 6 AND 12),
  title jsonb NOT NULL,
  is_personal boolean NOT NULL DEFAULT false,
  status text NOT NULL DEFAULT 'draft' CHECK (status IN ('draft','review','published')),
  prompt_version text NOT NULL,
  generation_seconds real,
  input_tokens integer NOT NULL DEFAULT 0,
  output_tokens integer NOT NULL DEFAULT 0,
  estimated_cost_usd real NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX courses_owner_idx ON public.courses (owner_id);

CREATE TABLE public.units (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  position smallint NOT NULL,
  title jsonb NOT NULL
);

CREATE TABLE public.lessons (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  unit_id uuid NOT NULL REFERENCES public.units (id) ON DELETE CASCADE,
  position smallint NOT NULL,
  title jsonb NOT NULL,
  summary text NOT NULL DEFAULT '',
  generation_status text NOT NULL DEFAULT 'not_started'
    CHECK (generation_status IN ('not_started','generating','ready','failed','needs_review')),
  review_status text NOT NULL DEFAULT 'unreviewed'
    CHECK (review_status IN ('unreviewed','reviewed')),
  published_at timestamptz,
  prompt_version text
);
CREATE INDEX lessons_course_idx ON public.lessons (course_id, position);

CREATE TABLE public.questions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  lesson_id uuid NOT NULL REFERENCES public.lessons (id) ON DELETE CASCADE,
  position smallint NOT NULL,
  type text NOT NULL CHECK (type IN ('multiple_choice','match_pairs','fill_formula','true_false')),
  content jsonb NOT NULL,
  source_page smallint NOT NULL,
  confidence real NOT NULL,
  flag_reason text,
  call_safe boolean NOT NULL,
  call_script jsonb NOT NULL,
  teacher_edited boolean NOT NULL DEFAULT false,
  prompt_version text NOT NULL
);
CREATE INDEX questions_lesson_idx ON public.questions (lesson_id, position);

CREATE TABLE public.terms (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  key text NOT NULL,
  content jsonb NOT NULL,
  source_page smallint NOT NULL,
  prompt_version text NOT NULL,
  UNIQUE (course_id, key)
);

CREATE TABLE public.course_assignments (
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  classroom_id uuid NOT NULL REFERENCES public.classrooms (id) ON DELETE CASCADE,
  due_at timestamptz,
  assigned_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (course_id, classroom_id)
);
"""

HELPERS = """
-- Owner of the course, or a student in a classroom it's assigned to.
CREATE OR REPLACE FUNCTION app_private.can_read_course(cid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM courses WHERE id = cid AND owner_id = auth.uid())
      OR EXISTS (SELECT 1 FROM course_assignments a
                 JOIN memberships m ON m.classroom_id = a.classroom_id
                 WHERE a.course_id = cid AND m.student_id = auth.uid() AND m.status = 'active')
$$;

CREATE OR REPLACE FUNCTION app_private.owns_course(cid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM courses WHERE id = cid AND owner_id = auth.uid())
$$;

-- A lesson is readable by its course owner, or by assigned students once it's published.
CREATE OR REPLACE FUNCTION app_private.can_read_lesson(lid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (
    SELECT 1 FROM lessons l
    WHERE l.id = lid
      AND (app_private.owns_course(l.course_id)
           OR (l.published_at IS NOT NULL AND app_private.can_read_course(l.course_id)))
  )
$$;

CREATE OR REPLACE FUNCTION app_private.owns_lesson(lid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM lessons l JOIN courses c ON c.id = l.course_id
                 WHERE l.id = lid AND c.owner_id = auth.uid())
$$;

CREATE OR REPLACE FUNCTION app_private.owns_upload(uid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM uploads WHERE id = uid AND owner_id = auth.uid())
$$;

REVOKE ALL ON ALL FUNCTIONS IN SCHEMA app_private FROM PUBLIC;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app_private TO authenticated;
"""

# Kept in sync with supabase/policies/courses.sql (a test checks they match).
COURSE_POLICIES = """
ALTER TABLE public.uploads ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.upload_pages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.note_chunks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.courses ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.units ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lessons ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.terms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.course_assignments ENABLE ROW LEVEL SECURITY;

CREATE POLICY uploads_owner ON public.uploads
  FOR ALL TO authenticated
  USING (owner_id = auth.uid())
  WITH CHECK (owner_id = auth.uid());

CREATE POLICY upload_pages_owner ON public.upload_pages
  FOR ALL TO authenticated
  USING (app_private.owns_upload(upload_id))
  WITH CHECK (app_private.owns_upload(upload_id));

CREATE POLICY note_chunks_owner ON public.note_chunks
  FOR SELECT TO authenticated
  USING (app_private.owns_upload(upload_id));

CREATE POLICY courses_select ON public.courses
  FOR SELECT TO authenticated
  USING (app_private.can_read_course(id));

CREATE POLICY courses_update ON public.courses
  FOR UPDATE TO authenticated
  USING (owner_id = auth.uid())
  WITH CHECK (owner_id = auth.uid());

CREATE POLICY units_select ON public.units
  FOR SELECT TO authenticated
  USING (app_private.can_read_course(course_id));

CREATE POLICY lessons_select ON public.lessons
  FOR SELECT TO authenticated
  USING (app_private.can_read_lesson(id));

CREATE POLICY lessons_update ON public.lessons
  FOR UPDATE TO authenticated
  USING (app_private.owns_course(course_id))
  WITH CHECK (app_private.owns_course(course_id));

CREATE POLICY questions_select ON public.questions
  FOR SELECT TO authenticated
  USING (app_private.can_read_lesson(lesson_id));

CREATE POLICY questions_write ON public.questions
  FOR ALL TO authenticated
  USING (app_private.owns_lesson(lesson_id))
  WITH CHECK (app_private.owns_lesson(lesson_id));

CREATE POLICY terms_select ON public.terms
  FOR SELECT TO authenticated
  USING (app_private.can_read_course(course_id));

CREATE POLICY assignments_select ON public.course_assignments
  FOR SELECT TO authenticated
  USING (app_private.can_read_course(course_id));

CREATE POLICY assignments_write ON public.course_assignments
  FOR ALL TO authenticated
  USING (app_private.owns_course(course_id) AND app_private.teaches(classroom_id))
  WITH CHECK (app_private.owns_course(course_id) AND app_private.teaches(classroom_id));

GRANT SELECT, INSERT, UPDATE ON public.uploads, public.upload_pages TO authenticated;
GRANT SELECT ON public.note_chunks, public.units, public.terms TO authenticated;
GRANT SELECT, UPDATE ON public.courses, public.lessons TO authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON public.questions, public.course_assignments
  TO authenticated;
"""


def upgrade() -> None:
    op.execute(CREATE_TABLES)
    op.execute(HELPERS)
    op.execute(COURSE_POLICIES)


def downgrade() -> None:
    for table in (
        "course_assignments",
        "terms",
        "questions",
        "lessons",
        "units",
        "courses",
        "note_chunks",
        "upload_pages",
        "uploads",
    ):
        op.execute(f"DROP TABLE IF EXISTS public.{table}")
    for fn in ("can_read_course", "owns_course", "can_read_lesson", "owns_lesson", "owns_upload"):
        op.execute(f"DROP FUNCTION IF EXISTS app_private.{fn}(uuid)")
