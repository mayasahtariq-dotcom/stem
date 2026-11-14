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
