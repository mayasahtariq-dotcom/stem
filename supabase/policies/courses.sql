-- Row-Level Security for uploads, note chunks, courses, units, lessons, questions, terms and
-- course assignments. Applied by Alembic migration 0003_courses
-- (backend/alembic/versions/0003_courses.py), which also creates the app_private helpers used
-- here. Kept in sync with COURSE_POLICIES there; a test checks they match.
--
-- Owners see their own uploads and courses. Students see a class course's published lessons
-- only when the course is assigned to a classroom they belong to. Personal courses are
-- owner-only.

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
