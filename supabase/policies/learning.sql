-- Row-Level Security for answer attempts, lesson progress, student stats, saved terms, doubts
-- and the audio cache. Applied by Alembic migration 0004_learning
-- (backend/alembic/versions/0004_learning.py), which also creates the app_private helper used
-- here. Kept in sync with LEARNING_POLICIES there; a test checks they match.
--
-- Students see only their own rows. A teacher can read attempts and progress on lessons of
-- their own courses. audio_cache has no policies: only the backend's service role uses it.

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
