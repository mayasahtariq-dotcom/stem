-- Row-Level Security for public.users: each user reads and updates only their own row.
-- Applied by Alembic migration 0001_users (backend/alembic/versions/0001_users.py).
-- Kept in sync with USERS_POLICIES there; backend/tests/test_migrations.py checks they match.

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_select_own ON public.users
  FOR SELECT TO authenticated
  USING (auth.uid() = id);

CREATE POLICY users_update_own ON public.users
  FOR UPDATE TO authenticated
  USING (auth.uid() = id)
  WITH CHECK (auth.uid() = id);

GRANT SELECT, UPDATE ON public.users TO authenticated;
