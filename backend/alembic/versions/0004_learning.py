"""student learning: answer attempts, lesson progress, stats, saved terms, doubts, audio cache

Revision ID: 0004_learning
Revises: 0003_courses
Create Date: 2026-11-15
"""

from collections.abc import Sequence

from alembic import op

revision: str = "0004_learning"
down_revision: str | None = "0003_courses"
branch_labels: str | Sequence[str] | None = None
depends_on: str | Sequence[str] | None = None

CREATE_TABLES = """
CREATE TABLE public.answer_attempts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  question_id uuid NOT NULL REFERENCES public.questions (id) ON DELETE CASCADE,
  lesson_id uuid NOT NULL REFERENCES public.lessons (id) ON DELETE CASCADE,
  correct boolean NOT NULL,
  first_try boolean NOT NULL,
  answer jsonb NOT NULL,
  mode text NOT NULL DEFAULT 'tap' CHECK (mode IN ('tap','voice','call')),
  time_ms integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX answer_attempts_student_idx ON public.answer_attempts (student_id, created_at);
CREATE INDEX answer_attempts_lesson_idx ON public.answer_attempts (lesson_id);

CREATE TABLE public.lesson_progress (
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  lesson_id uuid NOT NULL REFERENCES public.lessons (id) ON DELETE CASCADE,
  status text NOT NULL DEFAULT 'started' CHECK (status IN ('started','completed')),
  best_accuracy real NOT NULL DEFAULT 0,
  xp_earned integer NOT NULL DEFAULT 0,
  started_at timestamptz NOT NULL DEFAULT now(),
  completed_at timestamptz,
  PRIMARY KEY (student_id, lesson_id)
);

CREATE TABLE public.student_stats (
  student_id uuid PRIMARY KEY REFERENCES public.users (id) ON DELETE CASCADE,
  xp_total integer NOT NULL DEFAULT 0,
  hearts smallint NOT NULL DEFAULT 5 CHECK (hearts BETWEEN 0 AND 5),
  hearts_updated_at timestamptz NOT NULL DEFAULT now(),
  streak integer NOT NULL DEFAULT 0,
  longest_streak integer NOT NULL DEFAULT 0,
  last_active_date date,
  daily_goal smallint NOT NULL DEFAULT 50 CHECK (daily_goal IN (30, 50, 100)),
  xp_today integer NOT NULL DEFAULT 0,
  xp_today_date date
);

CREATE TABLE public.saved_terms (
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  term_key text NOT NULL,
  saved_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (student_id, course_id, term_key)
);

CREATE TABLE public.doubts (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  student_id uuid NOT NULL REFERENCES public.users (id) ON DELETE CASCADE,
  course_id uuid NOT NULL REFERENCES public.courses (id) ON DELETE CASCADE,
  question_id uuid REFERENCES public.questions (id) ON DELETE SET NULL,
  term_key text,
  mode text NOT NULL DEFAULT 'ask' CHECK (mode IN ('ask','simpler','example')),
  text text NOT NULL,
  answer jsonb NOT NULL,
  source_pages smallint[] NOT NULL DEFAULT '{}',
  in_notes boolean NOT NULL DEFAULT true,
  prompt_version text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX doubts_student_idx ON public.doubts (student_id, created_at);

CREATE TABLE public.audio_cache (
  hash text PRIMARY KEY,
  language text NOT NULL CHECK (language IN ('hi','kn','en')),
  voice text NOT NULL,
  model text NOT NULL,
  storage_path text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
"""

HELPERS = """
-- The current user owns the course that this lesson belongs to (teachers see their students'
-- progress on their own courses; used by M4's dashboard).
CREATE OR REPLACE FUNCTION app_private.owns_lesson_course(lid uuid) RETURNS boolean
LANGUAGE sql STABLE SECURITY DEFINER SET search_path = public, pg_temp AS $$
  SELECT EXISTS (SELECT 1 FROM lessons l JOIN courses c ON c.id = l.course_id
                 WHERE l.id = lid AND c.owner_id = auth.uid())
$$;

REVOKE ALL ON FUNCTION app_private.owns_lesson_course(uuid) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION app_private.owns_lesson_course(uuid) TO authenticated;
"""

# Kept in sync with supabase/policies/learning.sql (a test checks they match).
LEARNING_POLICIES = """
ALTER TABLE public.answer_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.lesson_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.student_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.saved_terms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.doubts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audio_cache ENABLE ROW LEVEL SECURITY;

CREATE POLICY attempts_own ON public.answer_attempts
  FOR ALL TO authenticated
  USING (student_id = auth.uid())
  WITH CHECK (student_id = auth.uid() AND app_private.can_read_lesson(lesson_id));

CREATE POLICY attempts_teacher ON public.answer_attempts
  FOR SELECT TO authenticated
  USING (app_private.owns_lesson_course(lesson_id));

CREATE POLICY progress_own ON public.lesson_progress
  FOR ALL TO authenticated
  USING (student_id = auth.uid())
  WITH CHECK (student_id = auth.uid() AND app_private.can_read_lesson(lesson_id));

CREATE POLICY progress_teacher ON public.lesson_progress
  FOR SELECT TO authenticated
  USING (app_private.owns_lesson_course(lesson_id));

CREATE POLICY stats_own ON public.student_stats
  FOR ALL TO authenticated
  USING (student_id = auth.uid())
  WITH CHECK (student_id = auth.uid());

CREATE POLICY saved_terms_own ON public.saved_terms
  FOR ALL TO authenticated
  USING (student_id = auth.uid())
  WITH CHECK (student_id = auth.uid() AND app_private.can_read_course(course_id));

CREATE POLICY doubts_own ON public.doubts
  FOR ALL TO authenticated
  USING (student_id = auth.uid())
  WITH CHECK (student_id = auth.uid() AND app_private.can_read_course(course_id));

GRANT SELECT, INSERT ON public.answer_attempts TO authenticated;
GRANT SELECT, INSERT, UPDATE ON public.lesson_progress, public.student_stats TO authenticated;
GRANT SELECT, INSERT, DELETE ON public.saved_terms TO authenticated;
GRANT SELECT, INSERT ON public.doubts TO authenticated;
"""


def upgrade() -> None:
    op.execute(CREATE_TABLES)
    op.execute(HELPERS)
    op.execute(LEARNING_POLICIES)


def downgrade() -> None:
    for table in (
        "audio_cache",
        "doubts",
        "saved_terms",
        "student_stats",
        "lesson_progress",
        "answer_attempts",
    ):
        op.execute(f"DROP TABLE IF EXISTS public.{table}")
    op.execute("DROP FUNCTION IF EXISTS app_private.owns_lesson_course(uuid)")
