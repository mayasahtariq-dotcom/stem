-- Row-Level Security for parent links, link codes, consent records, deletion requests and the
-- Student Twin, plus parent and teacher read access to student stats, progress and attempts.
-- Applied by Alembic migration 0005_parents_twin (backend/alembic/versions/0005_parents_twin.py),
-- which also creates the app_private helpers used here. Kept in sync with PARENT_POLICIES there;
-- a test checks they match.
--
-- Parents read only children whose link is active, and never doubts (no policy on doubts).
-- Writes go through the backend's service role.

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
