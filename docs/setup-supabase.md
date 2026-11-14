# Supabase setup (for M1 phone login)

## Step 1: Create a project (skip if you have one)
1. Go to https://supabase.com/dashboard → **New project**.
2. Name: `mooshala`. Database password: make a strong one and save it. Region: **South Asia (Mumbai)**.
3. Click **Create new project** and wait about 2 minutes.

## Step 2: Turn on phone login
1. Left menu → **Authentication** → **Sign In / Providers** (or **Providers**).
2. Click **Phone** → switch **Enable Phone provider** on.
3. Find **Test Phone Numbers and OTPs** and add your number like `919876543210=123456` (91 + your number, no `+`).
4. Click **Save**. If it won't save without an SMS provider, note exactly what it says.

## Step 3: Copy the keys into two private files
In Supabase: **Project Settings** (gear icon, bottom left) → **API** (also check **API Keys** and **JWT Keys**).

Open these files in Notepad or VS Code:
- `C:\Users\Taran\Desktop\Stem2\app\local.properties`
- `C:\Users\Taran\Desktop\Stem2\backend\.env`

| Replace this | With this from Supabase | In |
|---|---|---|
| `PASTE_PROJECT_URL_HERE` | Project URL, like `https://abcd1234.supabase.co` | both files |
| `PASTE_ANON_PUBLIC_KEY_HERE` | anon / public key | local.properties |
| `PASTE_JWT_SECRET_HERE` | JWT Secret (legacy) | .env |
| `PASTE_SERVICE_ROLE_KEY_HERE` | service_role key (secret) | .env |

Save both files. These files are never uploaded to GitHub.

**Never paste these keys into the chat.** The service_role key gives full access to the database.

When finished, type **done** in Claude Code.
