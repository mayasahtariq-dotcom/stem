-- Row-Level Security for classrooms, memberships and join attempts, plus the extra users
-- policies M1 needs. Applied by Alembic migration 0002_classrooms
-- (backend/alembic/versions/0002_classrooms.py), which also creates the app_private helper
-- functions used here. Kept in sync with CLASSROOM_POLICIES there; a test checks they match.
--
-- Teachers see only their own classrooms; students see only classrooms they've joined.
-- Memberships are visible to the student themselves and to that classroom's teacher.

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
