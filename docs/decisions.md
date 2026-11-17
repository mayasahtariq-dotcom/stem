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

## Mooshala web prototype bundled in debug builds

- The prototype (`docs/design/mooshala-code.md`) is copied to `app/app/src/debug/assets/mooshala/` and shown in a WebView from Profile → "Design prototype (web)". Debug builds only; release builds contain neither the files nor the screen.
- It is a design reference to look at on a phone, not a replacement for the native Compose screens. Its fonts load from Google Fonts, so it needs internet for the exact look.
- If `mooshala-code.md` changes, re-copy the three files.
- Not yet run on a device: this PC has no Android SDK, so it was not compiled locally; CI is the first build.
