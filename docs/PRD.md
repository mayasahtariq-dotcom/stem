# STEM Tutor App: PRD (all milestones)

# Overview

This plan is for building the STEM Tutor app with Claude Code. Claude Code should read this overview fully before any milestone, then work on one milestone at a time. Milestones are built strictly in order, and a milestone isn't started until every item on the previous one's checklist is done. Claude Code shouldn't build features from later milestones early, even if they look easy. Wherever this file says VERIFY, it must read the vendor's current documentation before writing code instead of guessing how an API works.

The app is a Duolingo-style learning app for Biology, Chemistry and Physics, built for students in Hindi- and Kannada-medium schools. Teachers photograph their class notes and AI turns them into short, interactive lessons. Every lesson is bilingual, either Hindi with English or Kannada with English, with the regional language shown first in bold and the English line always underneath. The app has three modes, Student, Teacher and Parent. Students who don't own a smartphone take the same lessons through phone calls handled by Exotel.

The work is split into eight milestones. M0 sets up the project, design system, bilingual text, backend skeleton and CI, and leaves only a developer demo screen. M1 adds phone login, the role and language picker, and creating and joining classrooms with a key, so teachers can create classes and students can join them. M2 lets teachers upload notes, have AI build a course, review it and publish it. M3 is the student learning loop: the lesson path, four question types, feedback, term cards, doubt chat, streaks, XP, hearts and read-aloud. M4 adds the teacher dashboard and Parent mode, where parents link to their child, give consent and see progress. M5 delivers lessons over phone calls. M6 adds push alerts, reminders and weekly SMS summaries. M7 adds spoken answers, voice doubts, offline lessons, revision and a leaderboard. The pilot demo is complete after M5; M6 and M7 happen during the pilot. This file covers the overview and every milestone from M0 to M7.

## Tech stack

The tech stack is fixed, and Claude Code should ask before changing any of it.

The mobile app is a native Android app in Kotlin with Jetpack Compose and a custom Material 3 theme. It targets Android 8.0 and above (minSdk 26) and uses an MVVM structure in which each screen gets its state from a ViewModel, which gets data from a repository. It uses Hilt for dependency injection, Navigation Compose with one navigation graph per role, Retrofit with OkHttp and kotlinx.serialization for networking, and supabase-kt only for phone OTP login. Room handles caching and offline data, DataStore holds settings, CameraX and Coil handle photos, and Media3 ExoPlayer plays audio. Simple formulas are shown as plain text with real subscripts and superscripts, and complex ones use KaTeX in a WebView. Firebase Cloud Messaging is added in M6. App tests use JUnit, Turbine, Compose UI tests and MockWebServer.

The backend is FastAPI on Python 3.12, with Supabase providing Postgres, phone OTP auth, file storage, Row-Level Security and pgvector. SQLAlchemy 2 and Alembic handle the database and migrations, and a Postgres-backed queue called procrastinate runs background jobs. Claude's API does note reading and content generation. Sarvam AI provides Hindi, Kannada and English speech, behind an interface so Bhashini can be swapped in later. Exotel handles calls and SMS from M5 onward. Backend tests use pytest with fake versions of every external service.

The app signs in through Supabase and receives a token, but every data read and write goes through the FastAPI backend with that token attached; the app never touches database tables directly. The backend checks the token and also relies on Row-Level Security as a second layer of protection.

## Repository layout

The repository has an `app` folder for the Android project, split into core modules (`core/designsystem`, `core/network`, `core/data`, `core/common`) and feature modules (`feature/onboarding`, `feature/student`, `feature/teacher`, `feature/parent`). A `backend` folder holds the API routers, AI prompts and pipeline, speech code, Exotel call code, background jobs, database models and tests. A `supabase` folder holds the security policies, and a `docs` folder holds this PRD plus `decisions.md`, `progress.md`, `translations-to-review.md` and a `demos` folder. A `CLAUDE.md` file sits at the root.

## Language rules

Language rules apply everywhere. Every user-facing string must exist in Hindi, Kannada and English: app interface strings live in Android resource folders (`values`, `values-hi`, `values-kn`), and course content is stored as JSON with separate `en`, `hi` and `kn` fields. A shared BilingualText component shows the regional line in bold above the muted English line, and it's used for every question, option, button and explanation. The app's language follows the user's chosen language mode, not the phone's system language. Science terms use the word students actually hear in class, which is often a transliteration such as माइटोकॉन्ड्रिया, with the formal translation kept on the term card. Formulas and units stay in standard notation in every language. Every Hindi and Kannada string Claude Code writes is treated as a draft and logged in `docs/translations-to-review.md` for a native speaker to check.

## Design

The design uses a light palette. The background is white (#FFFFFF). Main buttons, headers and finished lessons use baby blue (#A9CBF0), and the current lesson, class key and highlights use cream (#F3DFB8). Deep blue (#2C5A88) is used only for headings, links and outlines, green (#2E9E5B) is reserved for correct answers, and red (#E5484D) for wrong answers, hearts and alerts. Muted text is #5E6B64 and never lighter. Text on baby blue or cream is always dark, never white.

Regional lines use Noto Sans Devanagari or Noto Sans Kannada in bold at 16 to 18 sp, English lines use Noto Sans at 13 to 14 sp, and headings use Noto Sans extra bold at 20 to 24 sp. Hindi and Kannada text needs a line height of at least 1.5. Touch targets are at least 48 dp, each screen has one primary action, and the app says "Not quite" instead of "Wrong."

## Rules for Claude Code

Claude Code should follow these working rules and copy them into `CLAUDE.md`:

- Never commit secrets. Keys come from `local.properties` for the app and `.env` for the backend, with example files committed instead.
- Every external service (Claude, Sarvam, Exotel, Firebase) sits behind an interface with a fake version used in tests and debug builds.
- Every screen has its own state class, a ViewModel, a stateless screen composable and previews in both Hindi and Kannada.
- Every screen handles loading, empty, error and success states, so the user never sees a blank screen. No business logic lives in UI code.
- A milestone isn't done while any test fails.
- After each milestone, tick its checklist in `docs/progress.md`, write a short demo note in `docs/demos/` saying what to tap to see it working, and log any deviation from this PRD in `docs/decisions.md`.
- If an instruction is unclear or conflicts with a vendor's docs, choose the simplest safe option, log it in `docs/decisions.md`, and continue.

## Glossary

A few terms are used throughout. Language mode means either Hindi plus English or Kannada plus English. A classroom is a teacher's group of students, joined with an eight-character key like BI09-K4X2. Content is organised as course, unit, lesson and question. A term is a science word with its Hindi, Kannada and English forms, meaning, analogy and audio. A doubt is a student's question to the AI tutor. A source chip is the small "Your notes, page 2" label showing where an answer came from. A call-only student learns entirely through phone calls.

---

# M0: Foundation

M0 has no user-facing features. Its goal is an empty but well-organised Android app and backend that build, run and pass tests in CI, with the design system and bilingual text ready for later milestones. It depends on nothing. Login, roles, classrooms and real screens are out of scope and shouldn't be built yet.

## Android setup

Create the project with the core and feature modules from the overview and add the libraries from the tech stack using a Gradle version catalog (`libs.versions.toml`). There are two build types. The debug build uses fake backends unless a setting called `USE_REAL_API` is turned on in `local.properties`. The release build uses the real backend with code shrinking enabled. A `local.properties.example` file should list `API_BASE_URL`, `SUPABASE_URL`, `SUPABASE_ANON_KEY` and `USE_REAL_API`.

## Theme

The design system module defines the colours, typography and overall theme from the overview's design tokens. Only the regular, bold and extra-bold weights of Noto Sans, Noto Sans Devanagari and Noto Sans Kannada are bundled, to keep the app small.

## Components

Build these components now, each with previews in Hindi and Kannada (no dark theme needed):

- **BilingualText:** a bold regional line above a muted English line, in small, medium and large sizes.
- **ChunkyButton:** a Duolingo-style button with a thick bottom border that looks pressed when tapped. It comes in baby blue for primary actions, cream for highlights, neutral, and disabled, with a bilingual label.
- **AnswerOption:** a tappable answer card with default, selected, correct, wrong, correct-reveal and disabled states.
- **TopStatsBar:** shows streak, XP and hearts.
- **SourceChip:** shows which notes page something came from.
- **Loading, error and empty screens:** each with a bilingual message and a retry button.

A debug-only component gallery screen shows every component in both languages; this gallery is the M0 demo.

## Language mode

The language mode is stored in DataStore as either Hindi or Kannada, defaulting to Hindi. It's applied through Android's per-app locale setting (`AppCompatDelegate.setApplicationLocales`) so string resources follow the user's choice rather than the phone's language. English strings live in the default resource folder, with separate Hindi and Kannada folders. BilingualText takes the regional string from the active locale and the English string from the default resources, through a helper that returns both for any string resource. A toggle on the gallery screen switches modes live.

## Backend

The backend is a FastAPI app with a routers folder, settings loaded from the environment file, and structured JSON logging. A `GET /health` endpoint returns an "ok" status and the current version. A `GET /me` endpoint checks the Supabase login token and returns the user's ID, or a 401 error if the token is missing or invalid.

Interfaces for the AI provider (`LLMProvider`) and the speech provider (`SpeechProvider`) exist with fake implementations only, making no real calls yet. A `.env.example` file lists `DATABASE_URL`, `SUPABASE_URL`, `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_KEY`, `ANTHROPIC_API_KEY` and `SARVAM_API_KEY`. A Dockerfile and `docker-compose.yml` run Postgres locally.

## Database

Alembic is set up, and the first migration creates a `users` table with these fields:

- an ID matching the Supabase login account
- a name
- a unique phone number
- a list of roles (student, teacher or parent)
- the language mode, Hindi or Kannada, defaulting to Hindi
- a creation time

Row-Level Security on this table lets each user read and update only their own row.

## CI

GitHub Actions runs lint, unit tests and a debug build for the app (`./gradlew lint testDebugUnitTest assembleDebug`), and ruff, mypy and pytest for the backend, on every push and pull request.

## Docs

Create `CLAUDE.md` with the rules from the overview, `docs/progress.md` with a checklist per milestone, an empty `docs/decisions.md`, and `docs/translations-to-review.md`.

## Tests

- A UI test confirming BilingualText shows both lines and that switching the mode changes the regional line.
- A test that the language mode survives an app restart.
- Backend tests that `/health` returns success, and that `/me` rejects a missing token but accepts a valid test token.

## Done checklist

- [ ] The app launches to the component gallery, and every component renders in both languages with previews.
- [ ] Switching language updates the screen without a restart.
- [ ] The backend runs locally in Docker with `/health` and `/me` working.
- [ ] The `users` table exists with its security rule.
- [ ] CI is green for both app and backend.
- [ ] The docs files exist, and `progress.md` and the demo note are updated.

---

# M1: Login, roles and classrooms

M1 requires M0 to be complete. Its goal is that a person can sign in with a phone number and choose a role and language. A teacher can then create a classroom and share its join key, and a student can join it by typing the key or scanning a QR code. Uploading notes, courses, lessons, parent linking and dashboards are out of scope. A user can pick the Parent role now, but its home screen stays a placeholder until M4.

## First launch

The user sees a splash screen, then a phone number screen with the +91 prefix fixed, and taps Send OTP. On the next screen they enter a six-digit code, which should be read from the SMS automatically where possible, with a resend option after 30 seconds. They then pick a language from two large cards, "हिन्दी + English" and "ಕನ್ನಡ + English," pick a role from three cards (Student, Teacher or Parent), enter their name, and land on that role's home screen. Returning users go straight to the home of the role they last used.

## Teacher creates a class

From their home screen, the teacher taps Create classroom and fills in:

- a name, such as "Class 9-A Science"
- a subject: Biology, Chemistry or Physics
- a grade from 6 to 12
- the allowed language modes, both ticked by default

After saving, they see the join key in large text inside a cream box, a QR code beneath it, and Copy and Share buttons. Share opens Android's share sheet with a message containing the class name, the key and a link that opens the app, so the teacher can post it straight to WhatsApp.

## Student joins a class

From their home screen, the student taps Join a class and sees eight character boxes that capitalise letters, move to the next box automatically and insert the dash for them. They can also scan the QR code instead. Once all eight characters are entered, the app shows a preview of the class: its name, the teacher, the subject, the number of students and the languages available. Tapping Join adds the class to My classes.

Errors are always shown as clear bilingual messages, never a blank screen. The possible errors are:

- the key doesn't exist
- the key has expired
- the student is already a member
- the class doesn't allow the student's language mode
- the student has made too many attempts

## Screens

M1 adds the phone number, OTP, language, role and name screens. The student home shows a greeting, the My classes list, a Join a class button and a bottom bar with Home, Learn, Class and Profile, where Learn shows "coming soon" for now. The teacher home lists their classrooms with subject and student count, plus a Create classroom button. The parent home is a placeholder saying linking a child is coming soon.

There are also screens to create a classroom, show its key and QR, view a classroom's details and members, and join a class. The profile screen shows the user's name and phone, a language toggle, a switcher between roles the user has, an option to add a role, and sign out.

## Join keys

Keys use the format XXXX-XXXX. The first half is a two-letter subject code (BI, CH or PH) followed by the grade as two digits, giving codes like BI09, CH10 or PH08. The second half is four random characters drawn from the alphabet `ABCDEFGHJKMNPQRSTUVWXYZ23456789`, which has no zero, O, one, I or L, so nothing can be misread. A full key looks like BI09-K4X2. Keys are generated on the server and must be unique. A web link (`https://<domain>/join/<KEY>`) and an app link (`stemtutor://join/<KEY>`) both open the join screen with the key already filled in.

## Data

A new migration adds a `classrooms` table with these fields:

- the teacher
- name, subject and grade
- the unique join key and an optional expiry time
- the allowed language modes
- a creation time

A `memberships` table links students to classrooms with a join time and status, and a `join_attempts` table records attempts for rate limiting.

## Endpoints

- Any user can update their profile (name, language and roles) with `PUT /me` and fetch it with `GET /me`.
- Teachers can create a classroom and get back its key (`POST /classrooms`), list their classrooms with member counts (`GET /classrooms`), and open one of their own classrooms to see its details and members (`GET /classrooms/{id}`).
- Students can fetch a class preview by key (`GET /classrooms/preview/{key}`), join a class with a key (`POST /classrooms/join`), and list the classes they belong to (`GET /me/classrooms`).

## Rules

- Previewing and joining are limited to 10 attempts per user per hour; further attempts get a "too many requests" (429) response.
- Under Row-Level Security, teachers see only their own classrooms and students see only classrooms they've joined.
- The preview endpoint uses elevated access but returns only safe fields.
- Errors come back as short codes such as `KEY_NOT_FOUND`, which the app turns into bilingual messages.

## App structure

An auth repository wraps the Supabase login library and stores the session securely. A network interceptor attaches the login token to every request and refreshes it once if the server rejects it. Navigation starts with onboarding, then hands off to the student, teacher or parent section.

## Tests

- The key generator never uses zero, O, one, I or L, and produces unique keys across 10,000 generated.
- A student can't see another classroom's members.
- The eleventh join attempt in an hour is refused.
- The join screen shows the right error for each error code.
- The full onboarding flow, using fake login, ends on the correct home screen for each role.

## Done checklist

- [ ] Phone OTP login works on a real device with the real Supabase project.
- [ ] Language and role are chosen during onboarding and can be changed in Profile.
- [ ] A teacher can create a class, see its key and QR, and share it to WhatsApp.
- [ ] A student can join by typing the key, scanning the QR or tapping the link.
- [ ] Every join error shows a clear bilingual message.
- [ ] The security and rate-limit tests pass.
- [ ] `progress.md` is ticked and the M1 demo note is written.

---

# M2: Notes to course

M2 requires M0 and M1. Its goal is that a teacher photographs today's notes and, within a minute, sees an AI-made course outline and a first lesson, reviews it, fixes anything wrong and publishes it to a classroom. Students can also make private courses from their own notes. Playing lessons, doubt chat and audio are out of scope until M3. Call scripts are generated in this milestone but only used in M5.

## Teacher makes a course

From their home screen, the teacher taps a large cream "Make a course from notes" button. On the upload screen they add pages with the camera (several shots in a row), from the gallery, as a PDF, or by typing notes. The pages appear as a strip of thumbnails that can be dragged to reorder, tapped to crop or rotate, or deleted. The app checks each photo for blur on the device and, if a page looks blurry, asks whether to retake it or keep it. The teacher confirms or changes the subject and grade, chooses whether the course is for one of their classes or just for themselves, and taps "Make course."

The generating screen shows each step as it finishes: notes read with the page count, topics found, key terms found, and lesson 1 being made. The outline appears as soon as it's ready. The screen tells the teacher they can leave and will be told when lesson 1 is ready; in M2 this is an in-app banner, and push notifications come in M6.

The review screen shows the editable course title and the list of lessons. Each lesson card shows its title, question count and whether it is ready, generating or not started. Opening a lesson lists its questions, each showing the prompt and options in the teacher's language with English below, the correct answer ticked, and Edit, Redo and Delete buttons. Questions the AI wasn't confident about are listed first with a yellow banner explaining why, for example that handwriting on page 2 was unclear and asking whether a word is "stomata." Each lesson has its own Publish button, and the teacher picks which classroom to publish to. Published lessons then appear for that class's students.

## Student personal course

A student can tap "Make a course from notes" on their home screen and use the same upload screen, with "just me" fixed. There is no review step. The course appears in My courses with an "AI-made" badge.

## Screens

M2 adds screens to upload notes, crop a page, type notes, watch generation progress, review a course, review a lesson, edit a question in all three languages (including which option is correct), assign a course to classrooms with an optional due date, and list courses for teachers and for students. The student list is read-only until M3.

## AI pipeline

The pipeline runs as background jobs, and each step updates the upload's status so the app can check progress every two seconds. First, the strongest vision model reads each page and returns its text, formulas, diagram positions and a confidence score from 0 to 1. Next, the text is split into chunks and embedded so doubt chat can search it in M3. Then a strong model builds the outline: subject, grade, units and lesson titles in all three languages, and a list of key terms. A cheaper model writes the term cards. Finally, a strong model writes 6 to 10 questions for a lesson.

Generation is lazy. The first four steps and lesson 1 run straight away. Lesson 2 is generated only when the teacher opens lesson 1 in review or when any student starts lesson 1, and so on. The whole course is never generated upfront. Model names are settings, not hard-coded, and the prompts live as files under `backend/app/ai/prompts`. Every generated row records which prompt version made it.

## Question format

Every question comes back as JSON and is checked against a schema. If the JSON is invalid, the pipeline retries once and then marks the item as needing review. Each question has:

- its type: multiple choice, match pairs, fill the formula, or true/false
- the source page and a confidence score
- the prompt in English, Hindi and Kannada
- the options, which point to terms rather than holding raw text, with one marked correct
- a one-sentence explanation in all three languages
- a `call_safe` flag, false when the question needs a diagram or a complex formula
- a call script per language with the spoken prompt, options and explanation, used in M5

Match questions have 3 to 5 pairs. Fill-the-formula questions have a formula with a blank, the answer, and a few symbol chips to choose from. True/false questions have a statement in three languages and the answer.

Each term has a key, its English, Hindi and Kannada forms, transliterations (how it is written in Latin letters), the formal translation, a meaning and an everyday analogy in all three languages, related terms and a source page.

## Grounding

Every prompt tells the model to use only facts from the provided notes. When the notes are unclear, the model lowers its confidence and explains what's unclear in a flag reason. It never invents page numbers. Explanations are at most one sentence, and analogies use everyday Indian life such as a stove, cricket, a rickshaw or a kitchen.

## Data

New tables hold:

- uploads, with owner, optional classroom, pages, status and any error
- note chunks, with page, text, confidence and an embedding
- courses, with owner, upload, optional classroom, subject, grade, title, whether it's personal and its status (draft, review or published)
- units and lessons, with order, title, generation status, review status and prompt version
- questions, with order, type, content, source page, confidence, flag reason, the call-safe flag, call script, whether a teacher edited it, and prompt version
- terms, per course
- course assignments, linking a course to a classroom with an optional due date

Photos are stored privately in Supabase Storage under the owner and upload.

## Endpoints

The app can:

- create an upload and get signed upload links per page, then start the pipeline
- check an upload's status
- fetch a course with its units and lessons, and fetch a lesson with its questions
- edit, regenerate or delete a question
- publish a lesson, and assign a course to classrooms with an optional due date
- list a teacher's courses and a student's courses

## Rules

Photos are compressed on the phone to at most 1600 pixels on the long side before upload. An upload has at most 10 pages, and a PDF is at most 10 MB. Students never see unpublished lessons of class courses, enforced both in the endpoint and with Row-Level Security. For each course, the backend logs generation time, token cost and the share of questions teachers edited.

## Tests

- The full pipeline runs with the fake AI provider returning sample JSON, and the statuses move in the right order.
- Invalid JSON from the AI leads to one retry and then "needs review."
- A student gets a 403 error when fetching an unpublished lesson.
- Lesson 2 isn't generated until something triggers it.
- On the app side, reordering and deleting pages works, and flagged questions appear first in lesson review.
- A manual run with real Claude on three real handwritten note photos (some in Hindi, some in English), with the results saved in `docs/demos/M2-samples`.

## Done checklist

- [ ] A teacher uploads three photos and sees the outline and lesson 1 in under 60 seconds with real Claude.
- [ ] Flagged items appear first with a reason.
- [ ] A teacher can edit, regenerate and delete questions, and the changes are saved.
- [ ] Publishing to a classroom makes the course appear in that class's student list.
- [ ] A student can make a personal course and it shows the "AI-made" badge.
- [ ] All tests pass, `progress.md` is ticked and the demo note is written.

---

# M3: Student learning

M3 requires M0 to M2. Its goal is that a student opens the app, taps Continue, and plays a 5 to 10 minute bilingual lesson with mixed question types and instant feedback. Along the way they can look up terms, ask doubts, and earn streak and XP. Speaking answers, voice doubts, offline download, spaced revision and the leaderboard come in M7, as do the label-diagram and order-steps question types.

## Student home

The home screen greets the student by name and shows their streak. Below that is a daily goal card with a progress bar, such as 30 of 50 XP. Next comes a baby blue Continue card that points to the next unfinished lesson. Assigned class lessons come first, earliest due date first, then other class lessons, then personal courses. Then come My classes, each with the teacher's name and a "new lessons" badge, and My courses, each with its percentage complete. The cream "Make a course from notes" button from M2 stays on the screen, and the bottom bar has Home, Learn, Class and Profile.

## Lesson path

The Learn tab shows each course as a zig-zag column of round lesson nodes under a header card with the unit title and "Lesson 3 of 6." Finished lessons are baby blue with a tick. The current lesson is cream, larger and shows its topic. Locked lessons are grey with a lock, and tapping one says which lesson unlocks it rather than doing nothing. Below the path are a Revise button, disabled with "coming soon" until M7, and a Terms button that opens the glossary. Units carry a subject colour accent: a green tint for Biology, cream for Chemistry and blue for Physics.

## Lesson player

The top of the lesson shows streak, XP and hearts, a progress bar, and a close button that asks for confirmation before exiting. Each screen shows one question with a Check button that stays disabled until the student answers, plus Listen and Ask doubt buttons under every question. After Check, a feedback sheet slides up. Questions answered wrongly are asked once more at the end of the lesson. The end screen shows XP earned, accuracy and streak status, and returns to the path.

## Question types

- **Multiple choice:** three or four bilingual answer cards, graded on the chosen option.
- **Match pairs:** two columns of tiles. The student taps one on the left, then its partner on the right. Matched pairs fade out, and a wrong pair shakes red but costs no heart. The question is done when everything is matched.
- **Fill the formula:** a formula with a blank box and symbol chips to drag or tap into it.
- **True or false:** a statement with two large buttons.

Formulas use real subscripts and superscripts, such as H₂SO₄ and x², and fall back to KaTeX only when plain text can't show them.

## Feedback sheet

A correct answer shows a green sheet with "बहुत बढ़िया! Great! +10 XP," a one-line reason in both languages and a Continue button. A wrong answer shows a red sheet with "Not quite" and the lost heart, the correct answer, a one-line reason in both languages, an Explain button that opens doubt chat about the question, and Continue. Both sheets have an "Explain simpler" link that re-explains the idea at a lower reading level with an everyday analogy.

## Term cards and glossary

Tapping any underlined term in a question, option or explanation opens its term card. The card has a speaker button, the regional word in large text with the English word, a pronunciation spelling, the meaning and a "think of it like" analogy in both languages, related terms, and a source chip. It also has Save and Ask doubt buttons. The Terms screen lists saved terms and every term in the course, searchable in any of the three languages.

## Doubt chat

Doubt chat opens from a question, the feedback sheet or a term card, and its header shows what it's about. The student types a question, with quick buttons for Listen, Example and Explain simpler. Answers are bilingual with the regional language first, at most about three short sentences, and always end with a source chip. If the notes don't cover the question, the answer starts by saying so and then gives a general answer. Each student can ask up to 20 doubts a day, a setting that can be changed.

## Gamification rules

- **XP:** +10 for a correct first try, +5 for a correct retry, and a 20 XP bonus for finishing a lesson.
- **Hearts:** a student starts with five. A wrong answer costs one heart, except in match pairs. One heart refills every four hours, up to five. At zero hearts the lesson pauses and says when hearts refill; M7 adds practising to earn them back.
- **Streak:** goes up by one on any day (India time) the student finishes at least one lesson, and resets to zero after a missed day.
- **Daily goal:** 50 XP by default; the student can choose 30, 50 or 100 in Profile.

All of these are calculated on the server when answers are submitted, and the app only displays the results.

## Listen

The backend returns an audio link for any text in a given language. It generates the audio with Sarvam and caches it in storage under a hash of the language, text and voice, so repeated text returns the same file. When a lesson is published, a background job creates the audio for all its questions, options and terms, so playback is instant. The app plays audio with ExoPlayer and keeps up to 50 MB of it on the phone. Tapping the speaker plays the regional line and then the English line; long-pressing plays only the English.

## Data

New tables hold:

- answer attempts, with student, question, lesson, whether it was correct and on the first try, the answer, the mode (tap for now) and time taken
- lesson progress per student
- per-student stats: XP, hearts and when they last changed, streak, longest streak, last active date and daily goal
- saved terms
- doubts, with the question or term they're about, the text, the answer and the source pages
- the audio cache

Doubts are private to the student. Neither teachers nor parents can read them, which Row-Level Security enforces.

## Endpoints

The app can:

- fetch the student home (Continue card, classes, courses and stats)
- fetch a course's path
- start a lesson, which returns its questions and triggers generation of the next lesson if it's missing
- submit an answer and get back whether it was correct, the XP change, hearts and the explanation
- complete a lesson, which updates final XP and streak and returns the next lesson
- fetch a term, and save or unsave it
- ask a doubt or ask for a simpler explanation
- get speech audio for a text

## Doubt answers

To answer a doubt, the backend embeds the student's question and finds the four closest note chunks from the same upload. It sends those to Claude along with the current question and the student's language mode, and returns the answer in the regional language and English, the source pages, and whether the notes covered it. If they didn't, the answer is prefixed with the "your notes don't cover this" line.

## Tests

- The XP, heart and streak rules, including the India-time midnight edge and heart refill timing.
- A teacher and a parent can't read a student's doubts.
- Starting lesson N triggers generation of lesson N+1 exactly once.
- The audio cache returns the same link for the same text.
- On the app side, each question type grades correctly and shows the right feedback.
- A full lesson with the fake API ends with the right XP.
- A manual run of one full lesson in Hindi and one in Kannada on a 2 GB RAM phone, noting any stutter.

## Done checklist

- [ ] The Continue card picks the right next lesson.
- [ ] All four question types work in both language modes.
- [ ] Wrong answers show the correct one and come back at the end of the lesson.
- [ ] Term cards open from underlined terms, and saving works.
- [ ] Doubt answers are bilingual and cite a notes page.
- [ ] Streak, XP, hearts and daily goal follow the rules.
- [ ] Listen works on every text, with instant playback in published lessons.
- [ ] A lesson loads in under 3 seconds on slow 4G and runs smoothly on a 2 GB phone.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

---

# M4: Teacher dashboard and Parent mode

M4 requires M0 to M3. Its goal is that a teacher can see at a glance how the class is doing and who is falling behind, and that a parent can link to their child, give consent, and see a simple dashboard answering three questions: is my child practising, are they improving, and what is due. Alerts and weekly summaries come in M6, one-tap revision assignment in M7, and parent–teacher messaging later.

## Teacher class dashboard

Each classroom gets a Dashboard tab with the class name and key at the top, then three tiles:

- **Active this week:** students who finished at least one lesson in the last seven days, such as 32 of 38.
- **Accuracy:** first-try correct answers divided by first-try attempts over the last seven days.
- **Behind:** students with an assigned lesson past its due date, or idle for more than three days.

Below the tiles, weak topics lists the five lessons or terms with the lowest first-try accuracy across the class, each with a bar coloured red below 50%, amber from 50 to 75% and green above 75%. The students list shows each name with streak, current lesson and accuracy. Students idle for more than three days appear in red with how long they've been idle. The list is sorted with students who are behind first, then by name, and can be filtered by course. Tapping a student shows lessons completed and accuracy per lesson, last active date, streak and their three weakest topics, but never their doubt chat.

## Linking a parent

A student goes to Profile, taps "Link a parent," and sees a six-character code and QR code that last 24 hours. A parent, signed in with the Parent role from M1, taps "Add child," enters the code or scans the QR, sees the child's name and class, and taps Link. The child's profile then shows their linked parents with the last four digits of each phone number and a Remove option. One parent can link many children, and a child can have at most two parents.

## Consent

To follow India's DPDP Act rules on children's data, new student accounts start as pending consent. The student can see their class, but before starting lessons they see a message asking a parent to approve their account, with the Link a parent button. If the teacher has ticked "School has collected consent" in the classroom settings, confirming they have forms on file, students in that class skip this step.

When a parent links a child, they see a consent screen explaining what is collected (name, class and learning activity) and what isn't (no location, contacts or public profile), and tap "I approve." Later, the parent can revoke consent, which pauses the child's account, or request data deletion, which a background job completes within seven days.

## Parent dashboard

The parent home is the dashboard. If more than one child is linked, a row of name chips at the top switches between them. Cards show:

- **This week:** lessons completed with an up or down arrow compared with last week, time spent and streak.
- **Accuracy:** this week against last week, in plain words such as "Improving," "Steady" or "Needs practice."
- **Subjects:** the strongest and weakest topic in each subject.
- **Assignments:** each assigned lesson with its due date and whether it is done, pending or overdue (in red).
- **Classes:** each class with its teacher.

Everything appears in the parent's own language mode. A large "Listen to summary" button reads a three-sentence summary aloud for parents who prefer listening.

Parents can never see doubt chats, other students' data, class rank or the leaderboard, and they can't edit anything.

## Screens

M4 adds the class dashboard, student detail and classroom settings screens for teachers; the link-a-parent code, linked parents and consent-pending screens for students; and the parent dashboard, add child, consent approval and child settings screens for parents.

## Data

New tables hold:

- parent links, with parent, student, status (active or revoked) and consent time
- link codes, with expiry
- consent records, saying whether consent came from a parent or the school, and when it was given or revoked
- deletion requests

Classrooms gain a school-consent flag with who set it and when, and student accounts gain a consent status that starts as pending.

## Endpoints

- Teachers can fetch a class dashboard and a student's detail for their own classrooms, and turn school consent on.
- Students can create a link code, list their parents, and remove one.
- Parents can link with a code, approve consent, revoke consent, request deletion, list their children, and fetch a child's dashboard.

## Calculation and security

Dashboard numbers come from SQL views over answers and progress, cached for five minutes per classroom, with weeks running Monday to Sunday India time. Parents get read-only access to progress, stats, summarised answers and assignments, and only for children whose link is active. They get no access to doubts. Teachers see dashboards only for their own classrooms.

## Tests

- A parent can't read a child after the link is revoked.
- A parent can't read doubts.
- One teacher can't read another teacher's dashboard.
- Link codes expire after 24 hours and work only once.
- A pending student can't start a lesson unless the class has school consent.
- Dashboard numbers match a hand-calculated example.
- The parent dashboard shows the arrows correctly and has an empty state for a child with no activity.
- The full link, consent and dashboard flow works with the fake API.

## Done checklist

- [ ] The teacher sees the tiles, weak topics and students list, with idle students in red.
- [ ] A student creates a code, and a parent links, approves and sees the dashboard.
- [ ] A parent with two children can switch between them.
- [ ] "Listen to summary" plays in the parent's language.
- [ ] Revoking consent and requesting deletion both work.
- [ ] The consent gate blocks lessons until approval or school consent.
- [ ] Tests prove parents can't see doubts or other students.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

---

# M5: Call mode

M5 requires M0 to M4. Before writing any Exotel code, Claude Code must read Exotel's current documentation for the Calls/connect API, App Bazaar flows, the Passthru applet and the Voicebot applet with two-way streaming, along with Exotel's example code in the `exotel/Agent-Stream`, `exotel/Agent-Stream-echobot` and `exotel/voice-streaming` GitHub repositories. The Voicebot applet may need to be switched on by Exotel support, so that request should go in early.

The goal is that a student without a smartphone learns the same lessons through a phone call. The call reads questions aloud in their language, and the student answers with the keypad. Their progress shows on the teacher and parent dashboards like any other student's. Spoken answers and voice doubts on calls come in M7, and SMS summaries in M6, except the consent message needed here.

## Teacher adds a call-only student

In the classroom's students list, the teacher taps "Add student without phone app" and enters:

- the student's name and phone number
- their language
- call days, Monday to Saturday by default
- a call window, 5 pm to 7 pm by default, which must fall between 9 am and 9 pm
- optionally a parent's phone, with a "same number" option when the student uses a parent's phone

The student shows as "Waiting for consent" until consent arrives, then as active with a phone icon.

On the class dashboard and student detail, call-only students carry the phone icon. Their detail page adds answered and missed calls this week, minutes used against the weekly cap, last call time, and Pause calls and Call now buttons. If more than half of a lesson's questions aren't call-safe, the teacher sees a warning on that lesson in review saying it isn't suitable for phone students because it has too many diagram questions. Call-only students skip such lessons, and they are listed as skipped.

## Consent call

When a call-only student is added, the backend calls the parent's number, or the student's if they share one. In the chosen language, the call explains who is calling, the school and teacher, what the calls are for and when they will come, then asks the listener to press 1 to agree or 2 to decline. Pressing 1 records consent. If the classroom already has school consent, this call is skipped. An unanswered consent call is retried the next day for up to three days, after which the teacher sees "Consent not received."

## Scheduled calls

A scheduler runs every five minutes. It calls each active call-only student who is due a call: today is one of their call days, the time is inside their window, they haven't finished a lesson call today, and they're under their weekly minutes. For each, it creates a call session and asks Exotel's Calls/connect API to ring the student from our Exotel number. The request includes our lesson flow, the session ID as a custom field, and our status webhook. VERIFY the exact parameter names and casing in Exotel's docs.

Each student gets a random time within their window each day to spread the load, and the scheduler stays under Exotel's rate limit of roughly 200 calls a minute (VERIFY the limit for our account). An unanswered or busy call is retried after 20 minutes, up to twice, inside the window. Three days in a row without an answered call marks the student "Not reachable" on the teacher dashboard.

## Missed-call callback

A student can also give a missed call to our Exotel number. The inbound flow sends the caller's number to our missed-call webhook through a Passthru applet and hangs up; VERIFY how to end the call without it being answered or charged to the student. If the number belongs to an active call-only student, it's between 9 am and 9 pm, and they're under their cap, we call them back within 60 seconds. If it's outside hours, we send an SMS saying when we'll call, using a DLT-registered template (VERIFY template registration with Exotel). Unknown numbers are ignored. If one number belongs to several siblings, the call starts by asking which child is learning, for example "Press 1 for Ananya, 2 for Ravi."

## Exotel flow and WebSocket

The App Bazaar flow is a Voicebot applet pointing at our secure WebSocket address, followed by a Hangup applet. The WebSocket must be authenticated, either with a shared secret in the address or an IP allowlist; VERIFY which Exotel supports. The session is identified from custom parameters in the stream's start message, or by matching the call ID returned when we placed the call. VERIFY how the custom field reaches the stream.

From Exotel, the WebSocket receives:

- **connected and start:** we load the session, student and lesson and begin the greeting
- **media:** ignored in M5, used for speech in M7
- **dtmf:** keypad presses, passed to the state machine
- **mark:** tells us audio we sent has finished playing
- **stop:** we save and close the session

We send media messages carrying audio to play, and a mark after each prompt so we know when it ends. Where supported, we send a clear message to stop audio when the student presses a key mid-prompt; VERIFY this. Phone audio defaults to 8 kHz. Outgoing chunks must be multiples of 320 bytes and between 3.2 KB (100 ms) and 100 KB. VERIFY the encoding (likely 16-bit mono PCM) and how it's packaged.

## Call state machine

The call follows a fixed order in `backend/app/calls/state.py`:

1. A greeting.
2. A warm-up question the student got wrong last time, if there is one.
3. A new term.
4. Each question in turn. The call waits for a key press. If none comes within ten seconds, it repeats the question once, then marks it skipped and moves on. A valid key leads to feedback, then the next question.
5. A summary once the questions run out.
6. Hang up.

At any time, the student can press 9 to repeat the last prompt, 0 for a simpler explanation, # to skip the question, or * for voice doubts, which says "coming soon" until M7. Each call covers one lesson with five to eight call-safe questions and lasts about five to eight minutes. If a key arrives while a prompt is playing, we stop sending audio and handle the key straight away.

Every answer is saved immediately as an attempt marked as a keypad call answer. XP and streak follow the app's rules, but hearts never block a call lesson. If a call drops, the next call resumes at the next unanswered question of the same lesson. Every transition is unit-tested with a fake WebSocket.

## Call script

Here's how a call sounds in Hindi mode:

- **Greeting:** the student's name, their streak and today's topic, e.g. "नमस्ते अनन्या! आपका streak 5 दिन का है। आज हम 'कोशिका की संरचना' सीखेंगे।"
- **Term intro:** "आज का शब्द: माइटोकॉन्ड्रिया, English में Mitochondria। यह कोशिका का ऊर्जा केंद्र है, जैसे घर में चूल्हा।"
- **Question:** "सवाल 1। कोशिका का ऊर्जा केंद्र कौन सा है? Nucleus के लिए 1 दबाएँ। Mitochondria के लिए 2 दबाएँ। Ribosome के लिए 3 दबाएँ।"
- **Correct:** "बहुत बढ़िया! सही जवाब।" followed by the reason.
- **Wrong:** "सही नहीं। सही जवाब है 2, Mitochondria।" followed by the reason.
- **Summary:** "आपने 8 में से 6 सही किए। 70 XP मिले। कल फिर मिलते हैं!"

Question text comes from the call scripts and terms generated in M2. The greeting, feedback and summary are fixed templates in Hindi and Kannada. Only call-safe multiple-choice and true/false questions are used on calls in M5. True/false asks the student to press 1 for true and 2 for false. English terms are spoken in English after the regional word.

## Pre-recorded audio

When a lesson is published, a background job uses Sarvam to record every call prompt in each language at 8 kHz and stores the files by lesson, language, question and part. Personal bits like the student's name, streak and score are stitched from short clips, and each student's name is recorded once when they're added. Calls never use live text-to-speech; they only play these stored files, which keeps calls fast and costs predictable.

## Data

New tables hold:

- **call-only students:** classroom, name, phone, language, call days and window, parent phone, status (awaiting consent, active, paused, not reachable or consent refused), name audio, and who added them
- **call sessions:** purpose (consent or lesson), direction (outbound or callback), Exotel call ID, status, lesson, start and end times, seconds and cost
- **call events:** a debug log
- **call audio:** links to the stored files

The attempts table gains an optional call-only student field, with a rule that each attempt belongs to exactly one kind of student. The M4 dashboards must include call-only students in their counts, weak topics and lists.

## Endpoints

- Teachers can add a call-only student, edit or pause one, trigger a call now (inside allowed hours only), and view their details and call stats.
- Exotel calls our status webhook to update sessions and schedule retries, and our missed-call webhook through Passthru.
- Exotel's Voicebot connects to our live-call WebSocket.

## Cost and safety

Each call-only student has a weekly cap of 15 minutes by default, adjustable per classroom. Calls go only to numbers with consent on record, only inside the student's window, and never outside 9 am to 9 pm India time. VERIFY TRAI's rules on automated calls before the pilot. The cost of each call is recorded from Exotel's call details (VERIFY the field name), and teachers see total minutes and estimated cost per class. Call audio is never recorded in M5.

## Tests

- **State machine:** every transition, timeouts, repeat, simpler, skip, interrupting a prompt, and resuming after a drop.
- **Scheduler:** respects call days, windows, caps, retries and siblings on one number, and never calls outside 9 am to 9 pm.
- **Missed-call webhook:** queues a callback for known numbers, ignores unknown ones, and queues an SMS outside hours.
- **WebSocket handler:** replays a recorded event sequence through a fake Exotel client.
- **Audio chunker:** always produces chunks of the right size.
- **Manual (required):** a real end-to-end call to a real phone in Hindi and in Kannada, with answer delay, call length and any audio problems noted in `docs/demos/M5.md`.

## Done checklist

- [ ] A teacher adds a call-only student, and the consent call works with a key press of 1.
- [ ] A scheduled call arrives inside the window and plays a full lesson answered by keypad.
- [ ] A missed call leads to a callback within 60 seconds.
- [ ] Keys 9, 0 and # work, and pressing a key during a prompt interrupts it.
- [ ] A dropped call resumes at the right question next time.
- [ ] Call answers appear on teacher and parent dashboards with the phone icon.
- [ ] The weekly minute cap is enforced.
- [ ] Tests pass, the real-phone demo is recorded and `progress.md` is ticked.

---

# M6: Notifications

M6 requires M0 to M5. Its goal is that people come back without having to remember to open the app. Students get gentle reminders, teachers hear when generation is done or students fall behind, and parents get alerts and a weekly summary, by SMS if they don't have the app or their child is a call-only student. WhatsApp messages and parent–teacher chat are out of scope.

## Notification types

| Notification | Sent to | When | Channel |
|---|---|---|---|
| Lesson ready | Teacher | Lesson 1 of an upload is ready to review | Push |
| New lesson | Student | A lesson is published to their class | Push |
| Streak reminder | Student | 7 pm, if they haven't finished a lesson today and have a streak | Push |
| Due soon | Student | 24 hours before an assignment is due, if not done | Push |
| Idle | Parent | Child hasn't practised for three days | Push, or SMS without the app |
| Overdue | Parent | An assignment is past due | Push, or SMS without the app |
| Streak broken | Parent | A streak of five or more resets | Push |
| Not reachable | Parent and teacher | A call-only student hasn't answered for three days | Push or SMS |
| Weekly summary | Parent | Sunday at 6 pm | Push, or SMS without the app |
| Class behind | Teacher | Monday at 8 am, if any students are behind | Push |

Each notification goes in the user's language mode.

## Rules

- Each user gets at most two push notifications a day, apart from "lesson ready"; lower-priority extras are dropped.
- Nothing is sent between 9 pm and 8 am India time; anything due then waits until 8 am.
- Every type can be muted in Settings, except consent-related messages.
- "Push, or SMS" means a parent with the app and a valid push token gets a push; otherwise they get an SMS.
- Parents of call-only students always get SMS.

## App changes

The app integrates Firebase Cloud Messaging and registers its push token on login and whenever it changes. On Android 13 and later, it asks for notification permission after a student's first lesson or a teacher's first class, not at startup, and explains why first in bilingual text. There are separate notification channels for learning, class updates and parent alerts. Tapping a notification opens the right screen: a lesson, a review screen, or a specific child's dashboard. Each home screen gets a bell icon opening an inbox of the last 30 days of notifications with unread dots. A notifications page in Settings has a switch for each type relevant to the user's role.

## Weekly summary

The weekly parent summary must fit in 300 characters for SMS and use plain words. For example: "Ananya this week: 5 lessons (up 2), 78% correct, 6-day streak. Weak topic: Stomata. Due: Acids & Bases (Wed)." The Hindi and Kannada versions are DLT-registered templates with variables; VERIFY the DLT template rules with Exotel before writing them. The push version opens the parent dashboard. A parent with several children gets one message per child.

## Backend

New tables hold:

- devices with push tokens
- notification preferences per user and type
- notifications, with recipient, type, content, channel, status (queued, sent, failed or dropped), scheduled time and any error
- an SMS log, with template, variables, Exotel ID, status and cost

A dispatcher job runs every minute and sends due notifications, applying quiet hours, the daily cap and preferences. A daily job at 7 pm queues streak reminders, due-soon, idle, overdue and streak-broken alerts. A Sunday 6 pm job sends weekly summaries, and a Monday 8 am job sends teacher notices. Events from earlier milestones trigger lesson-ready, new-lesson and not-reachable notifications. Push and SMS each sit behind an interface with a fake version.

The app can register a device, fetch its inbox, mark a notification read, and read or update its preferences.

## Tests

- A notification due at 10 pm goes out at 8 am the next day.
- The third push of the day is dropped, lowest priority first.
- Muted types aren't sent.
- A parent without a push token gets an SMS, and a call-only student's parent always does.
- The weekly summary fits 300 characters even with a 25-character name.
- Tapping each type of notification opens the right screen.

## Done checklist

- [ ] Every notification type arrives as a push on a real device.
- [ ] Tapping a notification opens the right screen.
- [ ] The inbox and per-type settings work.
- [ ] The weekly SMS reaches a real phone in Hindi and in Kannada, with the DLT templates approved.
- [ ] Quiet hours and the daily cap are enforced.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

---

# M7: Voice, offline, revision and pilot features

M7 requires M0 to M6. It's larger than the others, so it is built as four parts in order: 7A, 7B, 7C and 7D, each with its own checklist.

## 7A: Voice in the app

The goal is that students can speak answers and doubts in Hindi, Kannada or English. A new "speak the answer" question type plays the question and lets the student tap the microphone and say the answer, and multiple-choice and true/false questions get a microphone button too, so the student can say an option instead of tapping it. In doubt chat, a microphone button records the question and shows the transcript for editing before it's sent. If the doubt was spoken, the answer plays aloud automatically.

The app records up to 15 seconds of mono audio at 16 kHz, uploads it, and gets a transcript from Sarvam through the speech interface. To check an answer, the transcript is lowercased and stripped of punctuation, then fuzzily matched against the correct term's English, Hindi and Kannada forms and all its transliterations. A similarity of 0.8 or more counts, so "Newton," "न्यूटन" and "ನ್ಯೂಟನ್" are all correct. If the transcript is low-confidence or doesn't match, the app says it didn't catch that and lets the student try once more, then shows the tap options. The tap options are always visible anyway. These answers are recorded as voice attempts, and audio is deleted within 24 hours of transcription. The app asks for microphone permission the first time the button is tapped, with a bilingual explanation.

### Done checklist 7A
- [ ] Spoken answers are graded correctly in all three languages, tested with 20 recorded samples per language stored in the backend test fixtures.
- [ ] Voice doubts work end to end with an editable transcript.
- [ ] The tap fallback is always available, and the microphone permission flow works.

## 7B: Voice on calls

The goal is that call-only students can say their answers, and press * to ask a doubt by voice. After the options are read, the call listens for speech for up to six seconds while also waiting for a key press, and whichever comes first wins. Pressing * plays "Ask your question after the beep," records up to 10 seconds, transcribes it, gets a grounded answer from Claude in at most two short sentences in the call language, turns it into 8 kHz speech and plays it, then returns to the lesson. This uses the incoming media from the Voicebot WebSocket, with simple energy-based detection to tell when the student has stopped talking.

The target is under 3 seconds from the end of the student's speech to the start of the answer for most doubts, and under 5 seconds for nearly all. If an answer takes more than 6 seconds, a short filler clip such as "एक सेकंड…" plays while the student waits. A call allows at most three voice doubts, and they count toward the daily doubt limit. These answers are recorded as voice call attempts, and call audio isn't stored. Before building this, Claude Code must compare Sarvam and Bhashini on 30 recorded 8 kHz call clips of mixed Hindi-English and Kannada-English speech, choose the one with the lower word error rate, and record the result in `docs/decisions.md`.

### Done checklist 7B
- [ ] The speech provider comparison is done and the decision is logged.
- [ ] Spoken answers work on a real call in Hindi and in Kannada.
- [ ] A voice doubt is answered within the latency target on a real call.
- [ ] The keypad still works if the student doesn't speak.

## 7C: Offline lessons

The goal is that students in low-connectivity areas can download a course and play lessons without internet. A Download button on a course saves its lessons, questions, terms, images and audio to the phone, showing the size first and downloading only on Wi-Fi unless the student allows mobile data. Offline, the lesson player, term cards and glossary work. Doubt chat says it needs internet and, with the student's permission, queues the doubt to send when they're back online. Answers are saved on the phone and synced when the connection returns, retrying with increasing delays, and XP and streak are shown straight away and corrected by the server afterwards.

Only generated, published lessons can be downloaded, and downloading also triggers generation of the next two lessons. Storage is capped at 300 MB, and the oldest completed course is removed first, with a notice. The server is always the source of truth. Each answer carries a unique ID made of the device ID and a local ID, so an answer synced twice is counted only once. Offline lessons count toward the streak by the time they were done on the phone, if that's within the last three days.

### Done checklist 7C
- [ ] A student downloads a course, finishes a lesson in airplane mode, reconnects, and progress and XP sync correctly.
- [ ] Repeated syncing never creates duplicate answers.
- [ ] The storage cap is enforced.

## 7D: Revision, new question types, leaderboard and class tools

**Spaced revision.** Every wrongly answered question and weak term goes into the student's revision queue, to be reviewed after 1, 3 and 7 days. A correct answer moves it to the next interval, and a wrong answer sends it back to one day. The Revise button on the lesson path now works and plays a session of 5 to 10 due items. Revision also refills hearts, one heart for every five correct revision answers, and a student with no hearts is offered practice to earn them back. For call-only students, the warm-up at the start of each call uses up to three due revision items instead of a single question.

**New question types.**
- **Label the diagram:** shows a diagram cropped from the notes with numbered pins. The student drags word chips into numbered slots, with one extra chip as a distractor. These questions are never call-safe.
- **Order the steps:** shuffled cards the student drags into order. On calls they're used only when there are four steps or fewer, with the student pressing the step numbers in order.

The M2 generator is updated to make both types, creating diagram questions only when a diagram crop exists.

**Leaderboard.** A weekly XP leaderboard covers one classroom only, showing the top ten and the student's own rank, with call-only students included and marked with a phone icon. Teachers can turn it off for a classroom, and parents never see it.

**Class tools.** Teachers can regenerate the join key without removing existing members, set an expiry date for it, require approval for new joins, and remove students. On the dashboard's weak topics, one tap creates a revision set from the three weakest topics and assigns it with a due date, and it appears on students' Continue cards.

### Done checklist 7D
- [ ] The revision schedule works, with unit tests for intervals and resets.
- [ ] Hearts refill through revision.
- [ ] Label-the-diagram and order-the-steps questions are generated and playable.
- [ ] The leaderboard covers only one class and can be turned off.
- [ ] Regenerating, expiring and approving keys, and removing students, all work.
- [ ] Assigning revision from weak topics works and appears on students' Continue cards.

## Done checklist for all of M7

- [ ] The 7A, 7B, 7C and 7D checklists are complete.
- [ ] The pilot metrics are logged: weekly active students per class, lessons per student per week, accuracy on repeated topics, the share of AI questions teachers edited (target under 20%), voice usage share, and call minutes per student.
- [ ] `progress.md` is ticked, with a demo note for each part.
