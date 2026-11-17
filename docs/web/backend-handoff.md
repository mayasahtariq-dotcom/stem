# Backend handoff for Mooshala Web (Phase 1)

Nov 17, 2026

This note is for whoever builds the backend that the web app will connect to. It lists what the web app (`apps/web`) already calls, what it assumes about endpoints that don't exist yet, and how to keep the two in sync.

## How the frontend stays in sync

The web app's API types are generated from the backend's own OpenAPI document, so they can't drift silently:

```
cd packages/api-client
pnpm export-openapi   # runs the FastAPI app and writes openapi/backend.json (needs backend/.venv)
pnpm generate         # regenerates src/generated/backend.ts
```

If a backend change breaks a screen, `pnpm typecheck` fails and points at it. The fakes used in development and tests (`apps/web/src/mocks/handlers.ts`) are typed against the same schemas.

Errors must keep the existing format: `{"detail": {"code": "SOME_CODE"}}` with the HTTP status from `backend/app/errors.py`. The web app turns each code into a bilingual message (`apps/web/messages/*.json`, `errors` namespace). A new code needs a line there.

## Endpoints the web app uses today (they already exist)

| Call | Used for |
| --- | --- |
| `GET /me` | Profile after sign-in: `registered`, `name`, `roles`, `language_mode` decide where the user goes |
| `PUT /me` | New users: name, language and role from onboarding |
| `GET /classrooms` | Teacher shell class switcher |
| `GET /classrooms/preview/{key}` | Join a class: preview, including the join errors |
| `POST /classrooms/join` | Join a class |

Every call sends `Authorization: Bearer <Supabase access token>`, which the backend already verifies (`backend/app/auth.py`).

## Phone OTP: nothing to build

The web PRD lists `POST /auth/otp` and `POST /auth/verify`, but the backend doesn't have them, and the Android app doesn't need them either: both apps sign in with **Supabase phone OTP** directly (`signInWithOtp`, then `verifyOtp` with `type: "sms"`), and the backend verifies the resulting JWT. The web app does the same, so phone login works against the real backend as soon as `NEXT_PUBLIC_SUPABASE_URL` and `NEXT_PUBLIC_SUPABASE_ANON_KEY` are set.

If the backend later wants to own OTP (for example to send SMS through Exotel), tell the frontend; it means one new class implementing `AuthGateway` (`apps/web/src/features/auth/gateway.ts`).

## Class key + name login: please build `POST /auth/class-key`

Shared school computers sign a student in with the class key and their name (web PRD section 2, "Login is by phone code (OTP), or by a class key and a name"). The PRD names the path but not the payloads, so the web app assumes the shape below. **Building it exactly like this means no frontend change.**

### Request

```http
POST /auth/class-key
Content-Type: application/json

{ "class_key": "BI09-K4X2", "name": "Ravi" }
```

- `class_key`: the classroom join key, already normalised by the app to `XXXX-XXXX` uppercase (same format and alphabet as `classrooms.join_key`, docs/PRD.md M1).
- `name`: 1 to 60 characters, trimmed, single spaces.
- No `Authorization` header (the student isn't signed in yet).

### Success: `200 OK`

```json
{ "access_token": "<JWT>", "token_type": "bearer", "expires_in": 28800 }
```

- `access_token` must be accepted by `get_current_user`, so every existing endpoint works with it. The simplest way is a JWT with `aud=authenticated`, `sub=<user id>`, `exp`, signed with the same key the backend already checks (HS256 `SUPABASE_JWT_SECRET`), or a Supabase session minted with the service role.
- The user behind it must look like any student to `GET /me`: `registered: true`, `roles: ["student"]`, a `language_mode` the class allows, and an active membership in that class. The same key plus the same name should return the same student again (the app relies on this to sign a student back in).
- `expires_in` is in seconds; 8 hours (one school day) is suggested. The app keeps this session in `sessionStorage`, so closing the browser signs the student out.

### Errors (same format as every other endpoint)

| Status | `detail.code` | App shows |
| --- | --- | --- |
| 404 | `KEY_NOT_FOUND` | "No class has this key" |
| 410 | `KEY_EXPIRED` | same message |
| 429 | `TOO_MANY_ATTEMPTS` | "Too many tries" (please rate limit per IP and per key; there's no user yet) |
| 422 | (FastAPI validation) | generic message |

Until the endpoint exists, a real backend answers 404 without a code, and the app shows "This way of signing in isn't ready yet. Use a phone number." It never fakes success.

### Open questions for the backend

- Should a name collision in the same class (two "Ravi"s) be allowed, or should the response ask for a second detail? The app currently has no second field.
- Consent: a class-key student still needs the DPDP consent gate before lessons (school-wide consent, `PUT /classrooms/{id}/school-consent`, already covers this).

If you choose a different shape, only `apps/web/src/features/auth/class-key.ts` and `packages/api-client/src/pending-auth.ts` change; once the endpoint is in `openapi/backend.json`, delete `pending-auth.ts` and use the generated types.

## Prefix

The web PRD says "REST under /v1". The current backend serves routes at the root (`/me`, not `/v1/me`). The web app uses whatever `NEXT_PUBLIC_API_BASE_URL` points at, so adding `/v1` later only means setting the base URL to `https://api.example/v1`.
