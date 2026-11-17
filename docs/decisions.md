# Decisions and deviations from the PRD

## M0

- **Gradle root is `app/`, application module is `app/app`.** The PRD names `app/` as the Android project folder and lists `core/*` and `feature/*` modules inside it, so the Gradle root lives at `app/` and the application module is nested at `app/app`.
- **Compose UI tests run on Robolectric.** CI's command is `./gradlew lint testDebugUnitTest assembleDebug`, which runs no instrumented tests. So the BilingualText UI test is a Compose test run on the JVM with Robolectric, which this command covers.
- **Language switching in tests.** The running app switches language with `AppCompatDelegate.setApplicationLocales`. Tests and previews use `ProvideLanguageMode` (a locale-overridden Context/Configuration), which exercises the same resource lookup without an Activity.
- **No `localeConfig` in the manifest.** The app's language comes only from the stored language mode. Declaring `localeConfig` would let Android 13+ system settings change the language behind the app's back. `setApplicationLocales` works without it, and the stored mode is re-applied at every app start (`StemTutorApp.onCreate`), so we don't use AppCompat's `autoStoreLocales` either.
- **Loading screen retry is optional.** The PRD says loading, error and empty screens each have a retry button. A retry on a loading screen rarely makes sense, so `LoadingScreen` shows the button only when `onRetry` is passed; error and empty screens always show it.
- **The debug gallery lives in the `debug` source set**, so release builds contain no gallery code. Release builds show a "coming soon" placeholder.
- **Supabase JWT verification accepts both key types.** Supabase now recommends asymmetric signing keys (ES256/RS256), verified against `<SUPABASE_URL>/auth/v1/.well-known/jwks.json`, and calls the HS256 shared secret legacy. `/me` accepts both: HS256 with `SUPABASE_JWT_SECRET` (as the PRD's env list implies) and ES256/RS256 via JWKS (cached 10 minutes, matching Supabase's edge cache). Tokens must carry `aud=authenticated`, `exp` and `sub`.
- **Local Postgres auth stubs.** The `users` table references `auth.users` and the RLS policies use `auth.uid()` and the `authenticated` role, which exist only on Supabase. Migration `0001_users` creates minimal stand-ins (`auth.users(id)`, `auth.uid()` reading `request.jwt.claim.sub`, the `authenticated` role) **only when `auth.users` doesn't already exist**, so the same migration runs on Supabase, docker-compose and CI.
- **`users.phone` is `varchar(20)`, `name` defaults to `''`, `roles` defaults to empty.** A row can exist before onboarding (M1) fills in the name and role.
- **Libraries declared before they are used.** supabase-kt (auth), Room, CameraX, Coil and Media3 are declared now, as M0 asks, but have no code until the milestone that needs them. Room's KSP compiler will be added with the first entity.
- **Library versions** are a known-compatible stable set (AGP 8.7.3, Kotlin 2.1.0, Compose BOM 2024.12.01, Gradle 8.11.1) rather than the newest, to keep the build reliable.
- **Verification limits for M0.** The development PC had no JDK, Android SDK or Docker. The Android build and tests were verified only in GitHub Actions, and Docker Compose was written but not run locally. The backend was tested locally, and the migration/RLS test runs in CI against a pgvector Postgres service.
## M0 restyle: Mooshala design (2026-11-14)

The user supplied a web prototype, `docs/design/mooshala-code.md` ("Mooshala"), and asked for its look to be applied to the app. They chose **Mooshala's design over the PRD's Design section**, and **look only**: its screens get built in the milestone that owns each one.

- **Palette replaced.** The PRD's baby blue, cream and deep blue are gone. The tokens are now Mooshala's: green `#55B752`/`#3F9A3D` (primary buttons, progress, correct), blue `#1499E0` (selection, current lesson, celebration screens), red `#E2574C` (wrong, hearts), gold `#FFC23A`, ink `#2E3135`, muted `#6B7682` (never lighter), line `#E7EAEE`, soft `#F2F4F6`, cream `#FFF8EE`, brown `#8A6650`. See `core/designsystem/.../theme/Color.kt`.
- **White text on green buttons** follows the prototype. Its contrast (about 2.6:1) is below WCAG AA even for large text. Flagged here so it can be revisited (for example a darker green face) if accessibility testing calls for it.
- **Fonts.** Headings, buttons and English lines use **Rubik** (one variable font file, weights 400–800, from google/fonts, OFL). Regional lines keep **Noto Sans Devanagari / Kannada** bold, since Rubik has no Indic glyphs. The Latin Noto Sans files were removed.
- **Button styles** are now Primary (green), White and Ghost, plus disabled, instead of the PRD's blue/cream/neutral. The 3D edge is 5 dp and the face drops 4 dp when pressed, as in the prototype.
- **Mascot.** The prototype's chibi cow is redrawn in Compose Canvas (`mascot/CowMascot.kt`) with 7 poses (wave, heart, thumbs, question, sleep, cool, sad) and its idle animations. The meadow background is `mascot/MeadowScene.kt`. The loading, error and empty screens use the cow (question, sad, sleep) with a speech bubble.
- **Sticker icons.** The prototype's 24×24 hand-drawn icons are generated into `icon/StickerIconData.kt` (53 icons) and replace emoji and Material icons in the HUD and source chip. `seeds`, `calc` and `cowface` were skipped, since they use computed SVG.
- **App name** is now "Mooshala" (launcher label; the prototype's name), with a green launcher background.
- **Not built (out of PRD scope or later milestones):** pet feeding and fullness, the food shop, collectable cow poses, quests, the exam season, cow and student naming, and the role screen. The PRD doesn't define the gamified pet features. They need a product decision and a PRD update before any milestone builds them.
- **Superseded:** the earlier note that the streak icon uses the 🔥 emoji. It is now the flame sticker icon.

## M1 (2026-11-14)

- **Started before M0 was fully ticked.** The user asked to begin M1 while three M0 checks still needed a device or Docker (the app opening on a phone, live language switching on a phone, the backend in Docker). Those checks remain open in `progress.md`.
- **Libraries added beyond the PRD stack, approved with the M1 plan:**
  - ML Kit barcode scanning, to scan the QR code with the CameraX already in the stack.
  - ZXing core, to draw the QR code.
  - Google Play services SMS User Consent API (`play-services-auth-api-phone`), to fill the OTP from the SMS. This needs no SMS permission; the user taps Allow once. Without Play services the user types the code.
- **Session storage.** The Supabase session is stored in DataStore, encrypted with an AES-GCM key held in the Android Keystore. No extra encryption library is used, since `security-crypto` is deprecated.
- **Debug builds use a fake login and fake data.** Any 10-digit number signs in with code `123456`, and classrooms live in memory, seeded with demo keys for each error. Set `USE_REAL_API=true` in `local.properties` to use Supabase and the backend.
- **RLS applies to backend queries.** The backend connects as an elevated role, so for normal requests it switches each transaction to the `authenticated` role with the caller's JWT claims (`SET LOCAL ROLE authenticated` plus `request.jwt.claims`). RLS then really is a second layer, as the PRD says. Only the class preview and join lookup use elevated access, and they return only safe fields.
- **Policy helper functions.** Policies that check membership or ownership call `SECURITY DEFINER` functions in an `app_private` schema, so policies never query each other's RLS-protected tables, which would recurse. One more of these, `join_key_taken`, checks key uniqueness across all teachers.
- **Extra users policies.** Teachers can read the names of students in their classrooms, and students can read their own teachers' names. Without this, the PRD's member lists and teacher names couldn't be shown under RLS.
- **Rate limit.** Preview and join share one budget of 10 per user per rolling hour. Each attempt is recorded in its own transaction, so failed attempts count too. The 11th gets 429 `TOO_MANY_ATTEMPTS`.
- **Preview returns join errors early.** Preview raises the same codes as join (`KEY_NOT_FOUND`, `KEY_EXPIRED`, `ALREADY_MEMBER`, `LANGUAGE_NOT_ALLOWED`), so the student sees the problem before tapping Join. Join checks them all again. Extra codes: `NOT_A_TEACHER`, `NOT_A_STUDENT` and `PROFILE_INCOMPLETE`.
- **The last-used role is stored on the device** (DataStore), not on the server. A reinstall lands on the first role in the profile.
- **Join links.** `stemtutor://join/KEY` works now. `https://<JOIN_LINK_HOST>/join/KEY` is wired with `autoVerify`, but `JOIN_LINK_HOST` defaults to the placeholder `mooshala.example`. Web links only open the app once a real domain is set in `local.properties` and hosts `/.well-known/assetlinks.json`. The QR code encodes the web link, and the scanner extracts the key from any text.
- **Onboarding flow test.** The PRD's "full onboarding flow, using fake login, ends on the correct home screen for each role" runs through the real ViewModels with the fake repositories (`OnboardingFlowTest`), not a full Hilt UI test, so it runs fast in `testDebugUnitTest`.
- **Out of scope here:** the pet, shop and quests from the Mooshala mockup. The role screen adds a Parent card, which the mockup doesn't have but the PRD needs. The parent home is a placeholder until M4, as the PRD says.
- **Local DB tests.** On Windows the backend tests start an embedded Postgres (`pgserver`, a Windows-only dev dependency). CI uses a Postgres service. `gen_random_uuid()` is built in, so no `pgcrypto` extension is needed.

## M2 preparation (2026-11-14)

- **OCR: Surya instead of Claude vision.** At the user's request, M2 will use open-source tools instead of the Claude API, and **Surya OCR** is chosen for reading note photos (Hindi, Kannada and English). It runs inside the Python backend, behind an `OcrProvider` interface with a fake for tests, so it can be swapped later (for example to PaddleOCR or a vision model) without app changes.
  - **Licence (VERIFY before the pilot):** Surya's code is GPL-3.0, which is fine for server-side use. Its model weights are free for personal use, research and small organisations, but larger commercial use needs a paid licence. Check the current terms on the Surya repository before any commercial launch.
  - **Known limits:** Surya is strongest on printed text, so messy handwriting may need retakes or teacher fixes in review. Low OCR confidence feeds the PRD's "flagged" questions.
  - **Still open:** the model that writes the outline, terms and questions (also to be open-source, e.g. Qwen, Gemma or Sarvam-M via a hosted API). It will be picked by comparing candidates on real note photos at the start of M2.

## M2 (2026-11-15)

- **Started before M1's device checks.** At the user's request, M2 began while M1's phone checks (real Supabase login, WhatsApp share, QR scan, links) were still open.
- **Open-source AI instead of Claude.** Every AI step sits behind an interface with a fake used in tests and debug builds:
  - **OCR:** Surya (see "M2 preparation" above). The PRD's "strongest vision model reads each page" becomes Surya text + confidence, plus Surya layout boxes for diagram positions. Formulas come through as OCR text, not separate formula objects.
  - **Text model:** any OpenAI-compatible chat API (`LLM_BASE_URL`, `LLM_API_KEY`), so OpenRouter, Together, Groq or a self-hosted vLLM/Ollama all work. The outline, terms and questions models are separate settings (PRD: "model names are settings"). Defaults are Qwen 2.5 72B/7B until the bake-off picks the winner.
  - **Embeddings:** BGE-M3 (1024 dimensions) via sentence-transformers, stored in pgvector.
  - Surya and BGE-M3 need PyTorch, so they're an optional `[ml]` extra. CI and tests use the fakes.
- **Model choice still open.** `backend/scripts/bakeoff.py` runs Surya plus each candidate model on `samples/*.jpg` and saves the results to `docs/demos/M2-samples/`. That's also the PRD's "manual run with real AI on three real handwritten note photos".
- **Storage.** Supabase Storage (private bucket, signed upload links) in production. `LocalStorage` (files on the PC, uploaded through a signed `PUT /local-storage/{token}`) for development, so the real pipeline can run without Supabase. The page files skip our API token.
- **Jobs.** procrastinate (Postgres queue) when `JOBS_INLINE=false`; jobs run in-process for tests and development. Jobs are queued after the response, and the upload is claimed in its own short transaction, so the request never holds a lock the job needs. An early version deadlocked on this; the claim fixes it.
- **Invalid AI output.** One retry, then the outline marks the upload `needs_review`, or a lesson becomes `needs_review`; term cards are skipped. Page numbers the AI invents are corrected to a real page, with lowered confidence and a flag. Questions from pages with OCR confidence below 0.6 are flagged automatically.
- **Lazy generation.** Lesson N+1 is generated when the teacher opens lesson N in review. A conditional UPDATE makes this happen exactly once. M3 will reuse `ensure_next_lesson` when a student starts a lesson.
- **Personal courses** skip review: their lessons are published right away and show the "AI-made" badge. A teacher can also choose "just for me".
- **PDFs** count as one page of the 10. Text PDFs are read directly; scanned ones go through OCR.
- **Students' course list** is read-only until M3, as the PRD says. Students don't see unpublished lessons of class courses at all (RLS), and a direct request gets 403 `LESSON_NOT_PUBLISHED`.
- **Library added:** `androidx.exifinterface`, a small AndroidX library, so phone photos aren't decoded sideways.
- **Ready banner.** When generation finishes after the teacher left the screen, an in-app banner appears at the top of any screen. Push notifications replace it in M6.
- **Page reordering:** long-press and drag a thumbnail sideways, or use the arrow buttons for accessibility.
- **Free OpenRouter models (2026-11-15).** The user has a free OpenRouter account with no credit, so the defaults are free models, each step a list tried in order:
  - **Outline and questions:** NVIDIA Nemotron 3 Ultra (550B), then Gemma 4 31B, then Nemotron 3.5 Super VL.
  - **Term cards:** Nemotron 3.5 Super VL first.

  Free models are often rate-limited, so the client moves to the next model on 429/5xx errors or empty replies (reasoning models can spend their budget thinking). `max_tokens` is now 12,000 to leave room for that.
  - **Measured on sample physics notes:** outline in about 27 s; lesson 1 (10 questions) in about 144 s. That's well over the PRD's 60 s target. Paid or less-busy models would be faster; revisit before the pilot.
- **HTTPS behind antivirus/network filters.** On the dev PC, Python's bundled certificate list rejected OpenRouter's certificate (likely a filter adding its own root certificate). The backend now uses the operating system's certificate store via `truststore`.
- **Term keys** are cleaned to letters, digits and underscores (the model returned keys like `newton's_first_law` and `kg_m/s`).
- **Surya pinned to 0.17.1 (2026-11-15).** From 0.20, Surya became a vision-language model that needs a separate inference server (vLLM on NVIDIA GPUs, or llama.cpp's `llama-server` on a CPU). Version 0.17.1 is the last with the classic detection and recognition models, which run on a CPU with PyTorch alone, so it's pinned. Diagram detection (Surya's layout model) is a separate, slower model, so it's off by default (`OCR_DETECT_DIAGRAMS=true` turns it on). Diagram positions stay empty until then.
- **Testing against a PC backend.** `backend/scripts/dev_server.py` runs an embedded Postgres, applies migrations and serves the API on the PC's Wi-Fi address, with local storage and the real AI.
  - Logins are still checked against the real Supabase project.
  - A local `auth.users` stand-in is filled when a user first saves their profile (`LOCAL_AUTH_STUB`, development only).
  - The debug app's **Profile → Server (testing)** switches between the built-in pretend AI and a server address, then restarts. Debug builds allow plain HTTP on the local network; release builds keep HTTPS only.
  - The CI-built APK gets the Supabase URL and the public anon key from repository secrets, which the user adds. Without them, "Real server" stays in pretend mode instead of crashing.
- **Library pins for Surya 0.17 (2026-11-15).** Surya 0.17 fails under transformers 5 (`pad_token_id` error), so `transformers>=4.56,<5` and `sentence-transformers>=3,<6` are pinned. Surya also needs `requests`, which its package doesn't declare.
- **Measured on the dev PC (CPU):** Surya loads in about 75 s and reads a page in about 150 s. On a typed trilingual test page, confidence was 0.99: English perfect, Hindi nearly (one anusvara lost), Kannada readable but with dropped conjuncts. A GPU, or a faster OCR choice, is needed before the pilot's "under 60 seconds" target is realistic.
- **Embeddings are switchable.** `FAKE_EMBEDDINGS=true` keeps fake embeddings while the OCR and text model are real. The dev server uses this by default (`--real-embeddings` loads BGE-M3, a 2.2 GB download needed for doubt chat in M3).
- **LLM timeout raised to 300 s** for slow free models.
- **End-to-end with real AI on the dev PC (2026-11-15).** A test page (English, Hindi and Kannada lines) went photo → Surya → free OpenRouter models → a course of 3 lessons, with 8 grounded questions of all four types in lesson 1. It took about 5 minutes (OCR 2 min, outline 35 s, terms 1 min, lesson 1 75 s). It works, but it's far from the 60 s target on a CPU with free models.
- **`PHONE_IN_USE` (409).** Saving a profile with a phone number another account already has used to fail with a 500; it now returns a clear code, shown bilingually in the app.
- **Dev server start-up retries** when Postgres takes longer than pgserver's 10 s (after an unclean stop, or with antivirus scanning the data files, as seen on the dev PC).

## M3: Student learning (2026-11-15)

- **Started before M2's device checks**, at the user's request (as M2 was before M1's). The M2 device items stay unticked in `progress.md`.
- **Listen uses Sarvam Bulbul V4 Flash** (`bulbul:v4-flash`, MP3), per the PRD, with voices `ritu_hi_edtech`, `chaitra_kn_narration` and `ritu_en_edtech` (checked in Sarvam's Voices guide). The user has the ₹100 free credit (about ₹2 per 1,000 characters), so:
  - **`TTS_PREGENERATE=false` by default:** each clip is made on the first Listen tap, then cached in storage under a hash of language, model, voice and text. The PRD's make-audio-at-publish job exists and runs when the setting is `true`. "Instant playback in published lessons" therefore needs the setting on (or a second tap).
  - The fake voice (a short silent WAV) is used whenever `SARVAM_API_KEY` is blank, in tests and in debug builds.
  - `POST /audio` is rate-limited per user (300 an hour, in memory) and capped at Sarvam's 2,500 characters.
- **Grading.** Students receive each question's content, including the right answer, because instant feedback and the match-pairs tiles need it on the phone. The server grades every answer again, and only the server awards XP, takes hearts and keeps the streak (PRD: "calculated on the server"). A student who reads the network traffic could see answers. That's acceptable for practice questions; revisit if lessons ever count towards marks.
- **Match pairs** are always finished by matching everything. The app sends the number of wrong pairs; a clean match counts as correct (XP), one with slips counts as a miss (asked again at the end) but never costs a heart.
- **Streak** days use India time (Asia/Kolkata). A missed day shows the streak as 0 at once, and the next finished lesson starts it again at 1.
- **XP replays.** Replaying a finished lesson earns XP again (as in Duolingo). Simple; revisit if a leaderboard (M7) makes farming a problem.
- **Doubt retrieval.** With real embeddings (BGE-M3), the five nearest note chunks are sent to the model. With fake embeddings (the CPU dev setup), the whole notes text is sent instead; notes are at most 10 pages, so this fits. Answers say "your notes don't cover this" first when the model reports so, and then have no page chip. Note chunks are owner-only under RLS, so the server reads them with the service session after checking the student can read the course.
- **Doubt limit:** 20 per India-time day (`DOUBTS_PER_DAY`). "Explain simpler" and "Example" count too, since each one calls the model.
- **Lazy generation for students.** Starting lesson N makes lesson N+1, as in M2's review. A personal course's next lesson is also started if a student reaches it before it was made; the app then says "getting this lesson ready".
- **Separate `LearningApi`** Retrofit interface for the M3 endpoints, with a 120 s read timeout because free models can take a while to answer a doubt.
- **Offline fake.** Debug builds in pretend mode get a built-in sample course ("The Cell", three lessons using all four question types) from `FakeLearningRepository`, with XP, hearts and streaks following the server's rules in memory. It doesn't include courses made with the fake notes flow.
- **Formulas** are plain text with Unicode subscripts and superscripts, which the generator already outputs (H₂O, x²). The KaTeX WebView fallback is deferred until a real course needs it.
- **Underlined terms** are found by matching each term's regional form in the regional line and its English form (whole words, any case) in the English line. Options that point to a term key are shown with the term's forms.
- **Revise** shows "coming soon" (M7), as the PRD says.
- **Libraries added:** `androidx.media3:media3-datasource` and `media3-database` (same version as `media3-exoplayer`, already in the stack) for the 50 MB on-phone audio cache.

## M4: Teacher dashboard, Parent mode and the Student Twin (2026-11-15)

- **The Student Twin (an addition to the PRD, asked for by the user).** A live model of what each student knows, built only from their answers:
  - **Mastery per concept** (each lesson and each course term) with Bayesian Knowledge Tracing: start 0.2, learn 0.15, slip 0.1, guess = 1/options (0.5 for true/false). Retries count half.
  - **Forgetting:** shown mastery = p × 2^(−days/half-life); the half-life starts at 3 days, doubles after a correct review on a later day (max 60), halves after a miss (min 1). `next_review_at` is stored for M7's revision.
  - **Misconceptions:** a multiple-choice miss whose chosen option is another term records a confusion pair ("mixes up mitochondria and nucleus").
  - **Habits:** usual practice hour, median answer time, active days in 14 (computed on request, not stored).
  - **Match pairs** only report how many wrong pairs there were, not which, so every term in a match-pairs question gets the same result (right if no slips).
  - It's a pure function of the answer log: `twin.rebuild()` replays `answer_attempts` and gives the same state (tested), so parameters can change later.
  - **Where it shows:** teacher student detail (3 weakest concepts, confusions), weak topics ("N students still find this hard"), the parent dashboard (strongest/weakest topic per subject, the summary), doubt chat (a private hint about what the student finds hard), and "You know this: N%" on term cards.
  - **Privacy:** learning data only, under the same consent; no doubt texts and no AI-written profile of the child; parents see summaries; deletion removes it; RLS lets the student, their teachers and actively linked parents read it, and only the backend writes it.
- **Weak topics and tiles use first-try accuracy**, as the PRD says (so a hand calculation can check them); the twin adds the low-mastery count beside them.
- **"Active this week" and the tile accuracy use the last seven days**, as the PRD's tile text says; the parent dashboard's "this week vs last week" uses Monday–Sunday weeks in India time.
- **Dashboard cache:** five minutes per classroom (and course filter), in the API process's memory (`DASHBOARD_CACHE_SECONDS`, 0 in tests). Switching school consent clears it. A shared cache (Redis) isn't needed for the pilot.
- **Authorisation:** teacher and parent endpoints check ownership or an active link through the user's own session (RLS), then read the class or child's data with the service session. RLS policies for parents and teachers on stats, progress, attempts and the twin are added too, and parents have no policy on doubts at all (tested).
- **Consent gate:** starting a lesson returns 403 `CONSENT_REQUIRED` unless the student is approved or one of their classes has school consent. **Existing student accounts start as pending too**, as the PRD says; for testing, the teacher ticks "School has collected consent" on the class (or a parent approves).
- **School consent** is a classroom flag with who set it and when; it isn't copied into per-student consent records.
- **A student stays approved while at least one active linked parent has consented**; removing that parent puts the account back to pending.
- **Parent summary:** three fixed-template sentences in English, Hindi and Kannada filled with the numbers and the twin's weakest topic (no AI model: free, consistent, and checkable). "Listen to summary" plays it through Sarvam (regional, then English).
- **Deletion:** the request is recorded at once (and processing stops: consent becomes revoked), then a job deletes the student's profile, which cascades to their learning data, twin, links and consent records. In development the job runs right away; in production it runs from the queue, well inside the PRD's seven days. VERIFY: deleting the Supabase auth account itself needs Supabase's admin API (`DELETE /auth/v1/admin/users/{id}`) and isn't done yet.
- **Link codes** use the class-key alphabet (no 0/O/1/I/L), last 24 hours and work once. The QR holds just the code; the parent's scanner reuses the class-join scanner with a different code pattern.
- **Offline fakes:** debug builds show a class of eight students and a parent with two children (one approved, one waiting); code `KAV234` links a third. Pretend-mode lessons aren't consent-gated.

## Web Phase 1: Foundation (2026-11-17)

Spec: the Mooshala Web PRD (uploaded to the project) and `docs/web/p2-frontend-plan.md`. Architecture: `docs/web/architecture.md`.

- **Web app added beside the Android app.** The repository had no web frontend. Following the web PRD's build spec, a pnpm workspace with Turborepo was added at the root (`apps/web`, `packages/tokens`, `packages/api-client`). The Android app, backend and their CI are untouched; the only edit to an existing file is new ignore lines in `.gitignore`.
- **Versions** pinned to the latest stable releases at the start: Next.js 16.4, React 19.3, Tailwind 4.3, three 0.187 with React Three Fiber 9.8 and drei 10.7, TanStack Query 5, Zustand 5, zod 4, next-intl 4, supabase-js 2, Storybook 10, Playwright 1.63 (matches the browsers on the build machine). Deliberately one major behind: **TypeScript 5.9** (7.0, the native compiler, isn't supported by the Next.js and ESLint tooling yet), **ESLint 9** (eslint-config-next targets 9), **MSW 2** (3.0 is days old), **Vitest 4**, **jsdom 26**.
- **3D farm (user's Phase 1 brief).** The user asked for a genuine 3D farming world as the student's main screen, with the rendering foundation and a small proof of concept in Phase 1. The plan doesn't mention 3D; the user's brief wins. React Three Fiber was chosen because the stack is React/Next.js. three.js loads only on the farm route (`next/dynamic`), never on the login pages, to protect the 3-second first-load budget on slow 4G.
- **Student shell: HUD and dock instead of the left rail.** The plan says "student left rail that becomes a bottom tab bar"; the user asked for no permanent dashboard sidebar and a game-style HUD with farm-themed navigation. The student shell is a full-viewport 3D farm with a floating HUD and a wooden bottom dock at every size. Teacher and parent shells follow the plan.
- **Login screens use the illustrated `MeadowScene`, not the 3D farm,** for the same load-time reason. The farm is the first thing a student sees after signing in.
- **Phone OTP through Supabase, not `POST /auth/otp` and `/auth/verify`.** The web PRD lists these endpoints, but the backend doesn't implement them; it verifies Supabase JWTs (`backend/app/auth.py`), and the Android app signs in with Supabase directly. The web app does the same. No invented endpoints are called.
- **Class key + name login: assumed contract.** `POST /auth/class-key` is named in the web PRD but has no implementation and no documented payload. The web app assumes `{class_key, name}` → `{access_token, token_type, expires_in}` with the existing error codes (`KEY_NOT_FOUND`, `KEY_EXPIRED`, `TOO_MANY_ATTEMPTS`), written up for the backend developer in `docs/web/backend-handoff.md`. The fakes implement it; against a backend without the route the app shows "not ready yet, use a phone number" and never fakes a success. Class-key sessions are kept in `sessionStorage`, so closing the browser on a shared computer signs the student out.
- **No `/v1` prefix.** The backend serves routes at the root; the PRD says `/v1`. The base URL is configurable, so a later prefix is a config change.
- **Join keys, QR and links.** Typed key and `/join/<KEY>` links work. QR scanning uses the browser's built-in `BarcodeDetector` (Chrome and Android); where it's missing the Scan button is hidden and typing the key always works. No QR library is shipped.
- **Role guard is client-side.** Sessions live in the browser (supabase-js storage), so `RoleGate` guards route groups on the client; the backend's RLS and role errors remain the real enforcement. `@supabase/ssr` (cookie sessions for server-side guards) is not used yet.
- **Forms use zod schemas in screen view-models,** not react-hook-form. The Phase 1 forms have one or two fields; react-hook-form is still in the stack for the bigger forms (Phase 5's call-student form).
- **Primary button colour.** White text on the PRD's grass `#58CC02` is 2.1:1, below WCAG AA. Buttons use a darker "grass strong" face `#2F7A00` (5.4:1) with a `#1F5200` lip; the bright grass stays for progress fills, correct states and the 3D world. Muted text uses `stone #6B6257` (5.7:1 on cream). A test checks every text pair.
- **Tokens as a Tailwind `@theme` block.** `packages/tokens/src/index.ts` is the source; `tokens.css` is generated, and a test fails if it drifts. The 3D world imports the same TypeScript values.
- **Art comes from the Mooshala prototype.** The 55 sticker icons and 7 Golu poses are extracted from `docs/design/mooshala-code.md` by `apps/web/scripts/extract-mooshala-art.mjs`, so web and Android share the same illustrations and no emoji are used. The prototype's `cowface` icon is skipped (it's a computed cow).
- **Placeholder 3D models and sounds are generated, not downloaded.** `tree.glb` and `hay-bale.glb` are built by a script with three's GLTFExporter (proving the GLB path end to end), and the tap and pop sounds are synthesised WAVs, so there are no third-party licences. Artists can replace them with files of the same name.
- **"Every component has a Storybook story that passes axe"** is checked by a Vitest test that composes every story (in Hindi and in Kannada) and runs axe-core in jsdom. Colour contrast can't be measured in jsdom, so it's covered by the token contrast test and by Playwright + axe on the real auth pages (WCAG 2.2 AA tags, no serious or critical issues).
- **Out of scope here (later phases):** live game stats in the HUD (Phase 2), streak/hearts/XP logic of any kind (server-owned), the Learn, Quests, League and Profile destinations (shown disabled in the dock), Golu growth and Lottie (Phase 4), language switching after onboarding (profile screen), PWA and offline (Phase 6).
- **Verification limits.** Everything ran in a Linux container with headless Chromium and software WebGL (SwiftShader); no real phone or GPU and no real Supabase project was used. The real-services path (`NEXT_PUBLIC_USE_FAKES=false`) is type-checked and unit-tested at the gateway level but not exercised end to end.

## Web Phase 2: Student core (2026-11-17)

- **The learning path lives in the 3D farm.** Lessons are wooden markers along the dirt path (a green flag when done, a glowing bouncing post for the current lesson, a stone with a lock when locked; at most 8 around the current lesson). Destinations: the **learning barn** (current lesson and chapter progress), the **signboard** (course and chapter progress, classes), the **farmhouse study** (word book), the **farm lab** and **Golu** (doubt chat), the **market stall** (gems), the windmill (daily goal) and the field (crops grow with course progress). `/farm/learn` lists the same path without 3D for keyboard, screen-reader and no-WebGL users.
- **Hearts refill every four hours, not every 30 minutes.** The web PRD says 30 minutes; the backend (`backend/app/learning/rules.py`) and the Android PRD say four hours. The server owns the number, so the web shows the server's `refill_at` and the fakes use four hours.
- **Daily goal choices are 30, 50 and 100 XP** (backend `DailyGoalIn`), not the web PRD's 20, 50, 100.
- **No combo or perfect-lesson XP.** The web PRD adds +2 XP per 3-combo and +10 for a perfect lesson; the backend doesn't. The player shows the combo flame (display only) and every XP number comes from the server (`xp_delta`, `xp_earned`).
- **Wrong answers return once.** A question answered "not quite" comes back at the end of the lesson as a retry (`retry: true`); if the retry is also wrong the lesson moves on, so a lesson always ends. Match pairs is always finished by matching everything; wrong pairs count as `mistakes` and cost no heart (backend rule).
- **Resuming without restarting.** An interrupted lesson (closed tab, reload, lost connection) is saved on the device for 24 hours. Resuming uses `GET /lessons/{id}` instead of `POST /lessons/{id}/start`, because a new start would reset the server's attempt window used for the lesson's accuracy. A failed answer is kept and offered again ("send again"); nothing is graded on the device.
- **Question content is validated.** `QuestionOut.content` is parsed with zod against the backend's question JSON; a malformed question is skipped instead of breaking the lesson. Correct answers in `content` are never used to grade: the server's `AttemptOut` decides, and only match pairs uses the pairing to give per-pair feedback (as the backend's grader defines left i ↔ right i).
- **Gems and the shop are mocked.** The backend has no game service. `GET /game/stats` and `POST /game/freeze` are named in the web spec; `GET /game/shop`, `POST /game/shop/{item}/buy` and `PUT /game/mascot/outfit` are not and were added as assumptions (contract in `packages/api-client/src/pending-game.ts`, handed off in `docs/web/backend-handoff.md`). The mock uses the spec's documented numbers (5 gems per lesson, freeze 100 gems, two freezes at most); shop prices are placeholders. With real services these calls fail and the market shows its error state; lessons never depend on gems.
- **Audio.** `POST /audio` is used for the speaker button; when the server returns no clip (the fake always does), the browser's own speech synthesis reads the line in hi-IN or kn-IN.
- **The fake backend survives a reload.** In fake mode its data is kept in `localStorage` (a real server keeps data too) and the menu has "Reset demo data".
- **Verification limits.** As in Phase 1: headless Chromium with software WebGL in a container, fakes only, no real phone.

## Web Phase 3: Teacher and parent (2026-11-17)

- **All teacher and parent screens use real backend endpoints.** Dashboard, student detail, school consent, courses, uploads, question edit/regenerate/delete, publish and assign, and the whole parent router exist in the backend; nothing in this phase is a pending endpoint. The fakes (`apps/web/src/mocks/teacher`, `mocks/parent`) port the backend's rules: 10 pages, 12 MB photos, 10 MB PDFs, pipeline steps, link codes that last 24 hours and work once, two parents at most, consent gating on `POST /lessons/{id}/start` (approved, or not revoked and in a class with school consent).
- **"Approve" a flagged question = save it unchanged** (`PUT /questions/{id}`), because the backend has no separate approve call and a save clears the flag and sets confidence to 1. **"Reject" = `DELETE /questions/{id}`**, behind a confirmation.
- **Publish needs every flag cleared.** The backend would publish a flagged lesson; the web keeps the button disabled until each flagged question is approved, edited, regenerated or removed (the PRD's "teacher reviews before students see it"). Publishing is idempotent on the server and the button locks while a request is in flight, so double taps don't publish twice.
- **Editing scope.** Prompt, statement, explanation (three languages), the correct option, true/false answer, formula and answer chip can be edited. Match-pair contents can't (they link to terms); a wrong pair is removed or regenerated.
- **Uploads.** JPG and PDF only (the backend stores photos as JPG). Pages are PUT one at a time to the signed links with `XMLHttpRequest` for progress; a failed page shows its own retry and the pipeline only starts once every page is stored. "Ready" is shown only when the server says `ready` and returns a course. In the fakes, a file whose bytes start with `FAIL` fails once (retry demo) and typed notes containing "fail" make the pipeline fail.
- **Fake student side plays only "The Cell".** Courses made or assigned in the fake teacher portal are stored and reviewable, but the fake student home serves the one seeded course. The real backend serves every assigned course.
- **Demo seed:** class 9-A has school consent on (so Asha can learn before a parent links); turning it off in the teacher overview pauses her lessons until Meena approves. Parent codes: `K7M4PX` (Asha), `EXP1RD` (expired), `FULL22` (child with two parents already).
- **Parent farm picture** is derived only from dashboard numbers (one crop per lesson this week, one flower per streak day), and milestones are read off the same data. No invented probabilities or scores.
- **Class switcher** selection is kept in a small persisted store so every teacher page reads the same class.

## Web Phase 4: Game layer (2026-11-17)

- **Still mocked: the backend has no game service.** Quests, the league, badges and Golu Pots use the spec's `GET /game/quests`, `GET /game/league`, `GET /game/badges` and `POST /game/pots/{id}/open`, plus the assumed `POST /game/quests/{id}/claim` (→ `{quest, stats}`). Payloads are in `packages/api-client/src/pending-game.ts` and the fakes in `apps/web/src/mocks/game/progress.ts`. With real services these panels show their error states; lessons never depend on them.
- **League = the village fair noticeboard,** ranked only by XP earned from lessons this week (Monday to Sunday, India time), never by gems; promote and demote zones come from the server. Call-only learners are marked with a phone. Reached from the signboard and the dock.
- **Quests pay once.** The claim button locks while a request is in flight and the server refuses a second claim (`ALREADY_CLAIMED`) or an unfinished one (`QUEST_NOT_COMPLETE`). Quest goals in the fake are placeholders; 20 gems per quest is the spec's number.
- **Badges are collectibles** on the profile shelf, each worth 50 gems once (spec). Badges Asha earned before the gems ledger existed are seeded at 0 gems so the demo balance stays 140.
- **Golu Pot** is a 3D clay pot next to Golu that wobbles while one is waiting (one per finished lesson). What's inside is decided by the server and fixed per pot (reopening can't reroll); the UI shows no odds or probabilities anywhere.
- **Mascot growth** (calf 0 XP, cow 500, champion 2,000; next stage 8,000 shown as the top) is shown in 3D (size, champion bell colour) and on the profile with progress to the next stage, all from `GameStats.mascot`.


## Web Phase 5: Call mode (2026-11-17)

- **The call-mode contract is pending.** The backend has no call-student endpoints. The web uses the contract in `packages/api-client/src/pending-calls.ts` (types plus `CALL_PATHS`) through `services.pending`. In fake mode MSW serves it from `apps/web/src/mocks/calls`. With real services the Calls page shows its error state. The endpoints are:
  - POST/GET `/classrooms/{id}/call-students`
  - PATCH `/call-students/{id}`
  - POST `/call-students/{id}/call-now`
  - GET `/call-students/{id}/call`
  - GET `/call-students/{id}/twin`
- **The rules are shared, pure code.** `apps/web/src/features/calls/rules.ts` holds them, and both the console and the fake use them:
  - the window must sit inside 9 am to 9 pm IST, default 5 to 7 pm, at least 30 minutes, end after start
  - days are Monday to Saturday
  - a 15-minute weekly cap, with the week starting Monday 00:00 IST
  - an unanswered call is retried after 20 minutes, at most twice, then the student is "Not reachable"
  - phone numbers are Indian mobiles (6 to 9 then 9 digits), accepting +91 or a leading 0
  - The 30-minute minimum window is our own choice (the spec gives none); it stops windows too short to ever be called.
- **The order of block reasons:** CALL_IN_PROGRESS, CALLS_PAUSED, CONSENT_PENDING, WEEKLY_CAP_REACHED, OUTSIDE_CALL_HOURS. The console disables "Call now" only for reasons it can see in the server's status (`statusBlock`). It does not check the time window itself, because the server owns the clock and answers OUTSIDE_CALL_HOURS.
- **"Connected" only comes from the server.** "Call now" shows only the state the server returned (202 with `dialing`), then polls `GET /call-students/{id}/call` every second while the call is live. The shown state only moves forward, by rank: dialing, then ringing, then connected, then a final state. A poll may skip a step (dialing straight to no_answer). A stale or out-of-order reply is ignored, as is a reply about an older call (compared by `started_at`). A second tap is ignored while the request is pending or a call is live, and the server also answers 409 CALL_IN_PROGRESS.
- **Statuses and error codes.** There are seven student statuses: waiting_consent, scheduled, in_call, retry_pending, not_reachable, paused and cap_reached. The UI labels include "Waiting for consent" and "Not reachable". It uses only the existing error codes: DUPLICATE_PHONE, CALL_IN_PROGRESS, OUTSIDE_CALL_HOURS, CONSENT_PENDING, WEEKLY_CAP_REACHED, CALLS_PAUSED.
- **The duplicate-phone check (fake)** rejects a phone that matches another call student in the same class, or an app user who is a member of the class.
- **Fake-only demo conventions** (`mocks/calls/state.ts`; not product behaviour):
  - A phone ending in 0 never answers.
  - A new student's consent arrives one minute after they are added.
  - Every completed call bills 2 minutes.
  - Timing: dialing 2 s, ringing 3 s, connected 12 s.
  - The seed is Lakshmi (4 minutes used), Raju (waiting for consent), Meera (not reachable) and Sunil (at the cap).
  - A fake-only **demo clock** (`PUT /__demo/clock`, the "Pretend it's 6 pm" card) moves the fake clock to the next 6 pm IST on a calling day, so calls can be tried at any hour. The card appears only when `NEXT_PUBLIC_USE_FAKES` is on.
- **Call query keys** (`callKeys`) live in `features/calls/api.ts`, not in the shared query-keys.ts, so this phase doesn't conflict with parallel work.
- **The nav was not edited.** The Calls section is at `/teacher/calls` (plus `/teacher/calls/new`); the nav entry and CURRENT_PHASE bump are left to the merge.
- **Phone e2e clicks.** As in the other specs, the phone e2e project presses buttons by focus plus Enter, because Pixel 7 emulation lets neighbouring labels intercept pointer clicks.


## Web Phase 6: Pilot readiness, demo cut (2026-11-17)

- **The scope was trimmed for a presentation.** The user asked for a demo-ready build quickly. Phase 6 shipped the offline answer queue and a desktop website layout. PWA install, notifications and preferences, and the deep accessibility and 3D performance audits are **not done yet** (see `docs/web/pilot-readiness.md`).
- **Laptops and desktops get a website layout.** From the `laptop` breakpoint (1024 px), the student shell shows a fixed white top bar with the brand, the section links (regional plus English) and the stats. The page sits below the bar, and the wooden bottom dock is hidden. Phones and tablets keep the game HUD and dock. The student pages widen to `max-w-4xl` on laptops. The teacher and parent shells were already website layouts.
- **Offline answers go to an outbox, never graded on the device.** The answer key isn't sent to the browser, so an offline answer can't be graded locally.
  - Offline, or while older items still wait (to keep their order), answers and lesson completions are saved in an on-device outbox (`lib/offline/outbox.ts`, localStorage).
  - The player moves on with a neutral "Saved on this phone" sheet. It records no grade, no retry, no combo and no heart.
  - A lesson finished offline shows a "rewards arrive when you're back online" screen.
- **When the outbox sends:** on start, on reconnect, when the tab becomes visible, and every 30 s while items wait. It sends oldest first.
  - A network error or 5xx stops the run, and the items wait for the next one.
  - A 4xx drops the item and counts it (for example, hearts ran out first). The banner then says how many answers couldn't be counted.
- **No duplicate rewards.** Every outbox item has an `Idempotency-Key`. The fake backend (`mocks/idempotency.ts`) answers a replayed key with the first response and doesn't run it again. The real backend must do the same; see backend-handoff.
- **Items belong to one user.** On a shared school computer, the next student never sends somebody else's saved answers.
- **A saved lesson resumes offline from the cached lesson.** A new lesson can't be started offline, because the server opens the attempt window.


## Farm visual upgrade (2026-11-17)

- **The user's AI-generated reference image is the visual target.** It was used for the look only; the scene, layout, assets and functions were kept.
- **New visuals** (`apps/web/src/three/world/atmosphere.tsx`):
  - a gradient sky dome with a sun glow
  - big horizon clouds and rolling distant hills for depth
  - a rainbow
  - butterflies that fly all over the farm (`wanderPath`)
  - soap bubbles with a rim sheen that drift up and loop forever (`bubbleAt`)
  - brighter, warmer sun and haze
- **All motion stops with reduced motion**, and the counts drop on the low quality tier.
- **The camera is lower**, so the sky and rainbow show (maxPolarAngle went from 0.42π to 0.46π).
- **Golu (the cow) moved to the foreground.** Golu now stands at (8, 11.5) at twice the herd's size, facing the barn, and the waiting pot moves with Golu. Cows got Holstein patches on the back and rump. This is the only layout change, and it was made because the user asked for the cow as the main foreground character.


## Connected modes: classes, analytics and materials (2026-11-17)

- **Student mode is unchanged**, apart from a new "My classes" card on the profile. The card links to the existing join flow (`/join`: code, preview, join).
- **Teachers have a Classes section** at `/teacher/classes`:
  - Create a class through `POST /classrooms`, which the backend already has.
  - Each class gets a unique join code, such as BI08-XXXX, with a copy button.
  - A class page shows the code, the roster, the class score, completion, a 6-week trend, scores by chapter and the most-missed questions.
- **The fake roster is the seeded roster plus real accounts** that joined with the code (`rosterOf`). A student who joins appears for the teacher straight away, with their real answers.
- **Pending contracts** in `packages/api-client/src/pending-teacher.ts`, served by the fakes:
  - `GET /teacher/overview`: totals across classes, by class and subject, doing well, needs attention, recent activity
  - `GET /classrooms/{id}/insights`
  - `GET /teacher/materials`
  - The most-missed questions are computed from real first-try answers. For the seeded class they're topped up with seeded history.
  - The trend and chapter scores are synthetic around the real class accuracy (fake only).
- **Uploads accept .txt files**, read in the browser as a typed page. The upload also sends `file_name` for each page, a requested addition to PageIn, so the Materials list can show file names and formats. Its statuses are draft, processing, review and published.
- **Mode visibility:** the sign-in card has a Student / Teacher / Parent switch, and every top bar shows a mode badge. The role page cards have illustrations and purposes.
- **Not built:** the student app has no video, reading or interactive lesson types (it has four quiz question types inside lessons). The new teacher screens have no Storybook stories yet.

## Mode screen opens the mode directly (demo build)
- The user asked to remove the phone step: picking Student, Teacher or Parent on "Who are you?" now signs straight into that mode's demo account (Asha, Ravi Sir, Meena) when fakes are on. With a real backend it still goes to phone login.
- Phone OTP and school-computer login stay reachable at /login for testing. This is a demo convenience, not real authentication.

## Class notes (teacher shares, students read)
- The user asked for teachers to upload notes and students to view them. Teachers get a Notes page (/teacher/notes): pick a class, add a title, type notes and/or attach one photo, PDF or text file (up to 1.5 MB in the demo). Students get a Notes item in their menu (/farm/notes) listing notes from every class they are in.
- This is separate from the AI course upload, which turns pages into lessons. Notes are shared as is.
- Not in the backend yet: `POST /classrooms/{id}/notes`, `GET /teacher/notes`, `GET /me/notes` (packages/api-client/src/pending-notes.ts). The fake backend keeps files as data URLs; a real backend should store them in object storage and return signed URLs.
