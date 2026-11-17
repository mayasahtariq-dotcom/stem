# Progress

One checklist per milestone, copied from `docs/PRD.md`. Tick items only when they're verified.

## M0: Foundation

- [ ] The app launches to the component gallery, and every component renders in both languages with previews. *(Builds in CI and the debug APK is produced; not yet opened on a device or emulator.)*
- [ ] Switching language updates the screen without a restart. *(Covered by unit and Compose tests in CI; needs a check on a device.)*
- [ ] The backend runs locally in Docker with `/health` and `/me` working. *(`/health` and `/me` verified locally without Docker; Docker isn't installed on the dev PC.)*
- [x] The `users` table exists with its security rule. *(Migration + RLS test passes against Postgres in CI.)*
- [x] CI is green for both app and backend.
- [x] The docs files exist, and `progress.md` and the demo note are updated.

## M1: Login, roles and classrooms

- [ ] Phone OTP login works on a real device with the real Supabase project. *(Supabase login code is written and the project's keys and phone provider were checked; not yet run on a device. Steps in `docs/demos/M1.md`.)*
- [ ] Language and role are chosen during onboarding and can be changed in Profile. *(Onboarding flow tested in CI for all three roles with the fake login; Profile changes need a device check.)*
- [ ] A teacher can create a class, see its key and QR, and share it to WhatsApp. *(Built; create + key generation tested; sharing needs a device.)*
- [ ] A student can join by typing the key, scanning the QR or tapping the link. *(Built; typing and key parsing tested; scanning and links need a device.)*
- [x] Every join error shows a clear bilingual message. *(JoinClassScreenTest, Hindi and Kannada.)*
- [x] The security and rate-limit tests pass. *(RLS member-visibility and 11th-attempt tests pass on Postgres in CI.)*
- [x] `progress.md` is ticked and the M1 demo note is written.

## M2: Notes to course

- [ ] A teacher uploads three photos and sees the outline and lesson 1 in under 60 seconds with real Claude. *(Claude replaced by open-source models at the user's request; needs the real-model run with `scripts/bakeoff.py` and timing.)*
- [x] Flagged items appear first with a reason. *(Tested in the app and the backend.)*
- [x] A teacher can edit, regenerate and delete questions, and the changes are saved. *(Backend tests on Postgres; app flow built on the same API.)*
- [x] Publishing to a classroom makes the course appear in that class's student list. *(Backend test; needs a device check.)*
- [x] A student can make a personal course and it shows the "AI-made" badge. *(Backend test; needs a device check.)*
- [x] All tests pass, `progress.md` is ticked and the demo note is written. *(51 app + 59 backend tests in CI.)*

## M3: Student learning

- [ ] The Continue card picks the right next lesson.
- [ ] All four question types work in both language modes.
- [x] Wrong answers show the correct one and come back at the end of the lesson. (ViewModel tests.)
- [ ] Term cards open from underlined terms, and saving works.
- [ ] Doubt answers are bilingual and cite a notes page.
- [x] Streak, XP, hearts and daily goal follow the rules. (Server-side, covered by tests.)
- [ ] Listen works on every text, with instant playback in published lessons.
- [ ] A lesson loads in under 3 seconds on slow 4G and runs smoothly on a 2 GB phone.
- [x] Tests pass, `progress.md` is ticked and the demo note is written. (Backend 93 tests; Android tests in CI. Device checks above need a phone.)

## M4: Teacher dashboard and Parent mode

- [ ] The teacher sees the tiles, weak topics and students list, with idle students in red.
- [ ] A student creates a code, and a parent links, approves and sees the dashboard.
- [ ] A parent with two children can switch between them.
- [ ] "Listen to summary" plays in the parent's language.
- [ ] Revoking consent and requesting deletion both work.
- [x] The consent gate blocks lessons until approval or school consent. (Backend tests; phone check pending.)
- [x] Tests prove parents can't see doubts or other students.
- [x] Tests pass, `progress.md` is ticked and the demo note is written. (Backend 114 tests; Android in CI. Phone checks above pending.)
- [x] Addition: the Student Twin (mastery, forgetting, confusions) powers weak topics, student detail, the parent summary and doubt chat (see decisions.md).

## M5: Call mode

- [ ] A teacher adds a call-only student, and the consent call works with a key press of 1.
- [ ] A scheduled call arrives inside the window and plays a full lesson answered by keypad.
- [ ] A missed call leads to a callback within 60 seconds.
- [ ] Keys 9, 0 and # work, and pressing a key during a prompt interrupts it.
- [ ] A dropped call resumes at the right question next time.
- [ ] Call answers appear on teacher and parent dashboards with the phone icon.
- [ ] The weekly minute cap is enforced.
- [ ] Tests pass, the real-phone demo is recorded and `progress.md` is ticked.

## M6: Notifications

- [ ] Every notification type arrives as a push on a real device.
- [ ] Tapping a notification opens the right screen.
- [ ] The inbox and per-type settings work.
- [ ] The weekly SMS reaches a real phone in Hindi and in Kannada, with the DLT templates approved.
- [ ] Quiet hours and the daily cap are enforced.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

## M7: Voice, offline, revision and pilot features

### Done checklist 7A

- [ ] Spoken answers are graded correctly in all three languages, tested with 20 recorded samples per language stored in the backend test fixtures.
- [ ] Voice doubts work end to end with an editable transcript.
- [ ] The tap fallback is always available, and the microphone permission flow works.

### Done checklist 7B

- [ ] The speech provider comparison is done and the decision is logged.
- [ ] Spoken answers work on a real call in Hindi and in Kannada.
- [ ] A voice doubt is answered within the latency target on a real call.
- [ ] The keypad still works if the student doesn't speak.

### Done checklist 7C

- [ ] A student downloads a course, finishes a lesson in airplane mode, reconnects, and progress and XP sync correctly.
- [ ] Repeated syncing never creates duplicate answers.
- [ ] The storage cap is enforced.

### Done checklist 7D

- [ ] The revision schedule works, with unit tests for intervals and resets.
- [ ] Hearts refill through revision.
- [ ] Label-the-diagram and order-the-steps questions are generated and playable.
- [ ] The leaderboard covers only one class and can be turned off.
- [ ] Regenerating, expiring and approving keys, and removing students, all work.
- [ ] Assigning revision from weak topics works and appears on students' Continue cards.

### Done checklist for all of M7

- [ ] The 7A, 7B, 7C and 7D checklists are complete.
- [ ] The pilot metrics are logged: weekly active students per class, lessons per student per week, accuracy on repeated topics, the share of AI questions teachers edited (target under 20%), voice usage share, and call minutes per student.
- [ ] `progress.md` is ticked, with a demo note for each part.


# Mooshala Web (P2 frontend)

## Web Phase 1: Foundation

Plan: `docs/web/p2-frontend-plan.md`. Demo: `docs/demos/web-phase-1.md`. Deviations: `docs/decisions.md` ("Web Phase 1").

- [x] `apps/web` on Next.js App Router, strict TypeScript, route groups (auth), (student), (teacher), (parent).
- [x] `packages/tokens` (colours, radii, fonts, spacing, elevation, motion) as CSS variables wired into Tailwind.
- [x] Fonts via next/font (Rubik, Noto Sans Devanagari, Noto Sans Kannada); next-intl with en, hi, kn; language follows the stored mode.
- [x] Core components with Storybook stories (ChunkyButton, AnswerOption, SpeechBubble, ProgressBar, TopStatsBar, StreakFlame, XPRing, HeartMeter, GemCounter, LeagueRow, QuestCard, BadgeTile, GoluMascot static, MeadowScene, BilingualText, SourceChip, StickerIcon, plus inputs, cards, dialog, toasts, screen states).
- [x] Motion presets with reduced-motion fades; sound manager that waits for a gesture and has one-tap mute.
- [x] Shells: student (3D farm, HUD, dock), teacher (top bar, class switcher, tabs), parent (single column).
- [x] Auth screens: language, role, phone OTP, school-computer class key + name, name for new users, join a class by key, link or QR.
- [x] Data layer: TanStack Query on the generated OpenAPI client, Zustand stores, MSW handlers, Sentry (opt-in by DSN).
- [x] 3D foundation (user brief): scene host with lifecycle, camera, lighting, shadows, quality tiers, GLB loading with fallbacks, pointer and keyboard interaction; proof-of-concept farm with terrain, barn, windmill, field, fence, trees, hay bales.
- [x] Done when: every component has a story that passes axe (Hindi and Kannada).
- [x] Done when: Playwright logs in by OTP and by class key against the fakes (desktop and phone).
- [x] CI workflow for web: lint, typecheck, unit tests, Storybook build, build, Playwright.
- [ ] CI run on GitHub (nothing has been pushed yet).
- [ ] Real Supabase OTP sign-in checked on a phone.
- [ ] `POST /auth/class-key` built by the backend (contract in `docs/web/backend-handoff.md`).
- [ ] Hindi and Kannada strings reviewed by native speakers.

## Web Phase 2: Student core

Demo: `docs/demos/web-phase-2.md`. Deviations: `docs/decisions.md` ("Web Phase 2").

- [x] 3D farm home: farmhouse, barn, lab, signboard, market stall, pasture with cows, chickens, ducks, trees, bushes, flowers, fences, path, crop field, pond and stream.
- [x] Idle life: cows blink, swish tails and graze; grass and flowers sway; butterflies, drifting clouds, flowing stream, pond ripples, windmill. All still with reduced motion.
- [x] Learning path as in-world destinations with done, current and locked lessons, chapter progress and "finish X first" prerequisites; `/farm/learn` list view.
- [x] Lesson player for multiple choice, match pairs, fill the formula and true/false on `POST /lessons/{id}/start`, `POST /attempts`, `POST /lessons/{id}/complete`.
- [x] Farm feedback: sprout blooms and Golu cheers for a right answer; plant bends and recovers, Golu thinks, explanation, "Not quite", retry at the end; basket fills at the end. No confetti.
- [x] HUD with streak, hearts, gems and daily-goal ring from the server; XP kept separate from gems; gems never block lessons.
- [x] Market stall: streak freeze and decorations bought with gems (mocked game endpoints), shown on the farm.
- [x] Word book (search in three languages, meaning, farm analogy, save) and doubt chat with Golu (notes page, simpler, example, daily limit).
- [x] Interrupted lessons resume; failed answers are kept and resent.
- [x] Tests: path, four question types, answer shapes, session rules, XP/streak/hearts parity with the backend rules, terms, doubts, persistence, 3D scene and interaction; Playwright student journey.
- [ ] Game endpoints built by the backend (`docs/web/backend-handoff.md`).

## Web Phase 3: Teacher and parent

Demo: `docs/demos/web-phase-3.md`. Deviations: `docs/decisions.md` ("Web Phase 3").

- [x] Teacher overview: tiles, who needs help, weak topics, class key, school consent toggle.
- [x] Students list (filter, search, sort) and student detail (lessons, weakest concepts, mix-ups, trend).
- [x] Courses list, course detail (rename, assign with due date, per-lesson publish).
- [x] Upload from JPG/PDF/typed notes with validation, per-page progress and retry, then the AI pipeline steps; no success shown before the server confirms.
- [x] Lesson review: flags and confidence, approve, edit, regenerate, reject with confirmation, publish once.
- [x] Parent: link by code with preview, consent approve/pause, unlink, deletion request, weekly summary with Listen, support areas, milestones, assignments, farm picture.
- [x] Student profile: make a parent link code, see and remove linked parents.
- [x] Tests: review/upload/student rules, teacher and parent flows against the fakes, stories with axe in hi and kn, Playwright teacher and parent journeys with axe (desktop and phone).

## Web Phase 4: Game layer

Demo: `docs/demos/web-phase-4.md`. Deviations: `docs/decisions.md` ("Web Phase 4").

- [x] Village fair noticeboard (weekly XP league, promote/demote zones, call-only marker, never ranked by gems).
- [x] Quest board with daily and weekly quests; claim once (duplicate claims refused).
- [x] Badge collection on the profile.
- [x] Golu Pot as a 3D pot on the farm; server-decided contents, no odds shown, opens once.
- [x] Golu's growth stages in 3D and on the profile.
- [x] Tests: game rules, fake contract (claims, ranking, badge gems, pots), quest/league/profile/pot flows, stories with axe, Playwright game journey.
- [ ] Game endpoints built by the backend (`docs/web/backend-handoff.md`).


## Web Phase 5: Call mode

Demo: `docs/demos/web-phase-5.md`. Deviations: `docs/decisions.md` ("Web Phase 5").

- [x] Calls section at /teacher/calls: student list with status, window, days, language and minutes used
- [x] Add a student without the app: name, +91 phone validation, language, window (default 5 to 7 pm, inside 9 am to 9 pm IST), Mon to Sat, duplicate-phone check, confirmation step
- [x] Call console: Call now polls the server and never shows "connected" early; repeat taps are blocked; pause and resume; weekly cap of 15 minutes; retries after 20 minutes, twice; Not reachable; Waiting for consent; learning summary ("twin")
- [x] Errors enforced: OUTSIDE_CALL_HOURS, CONSENT_PENDING, WEEKLY_CAP_REACHED, CALLS_PAUSED, CALL_IN_PROGRESS, DUPLICATE_PHONE
- [x] Stories (11, axe-tested in hi and kn), unit tests for the rules, integration tests against the fakes, e2e/calls.spec.ts (phone and desktop)
- [ ] Real backend endpoints (pending; see backend-handoff)


## Web Phase 6: Pilot readiness (demo cut)

Demo: `docs/demos/web-phase-6.md`. Deviations: `docs/decisions.md` ("Web Phase 6"). Honest status: `docs/web/pilot-readiness.md`.

- [x] Desktop website layout for the student area (top bar with links on laptops, dock only on phones and tablets).
- [x] Offline outbox: answers and completions saved on the device, sent in order on reconnect, with idempotency keys, so rewards are never paid twice. Per-user, so a shared computer is safe.
- [x] Offline banner (offline, syncing, couldn't be counted) and an offline lesson-finish screen.
- [x] Tests: outbox replay pays once (answer and lesson bonus), stops on network errors, drops refused items, ignores other users' items, and a full offline lesson synced on reconnect.
- [ ] PWA install (manifest, service worker, app-shell cache).
- [ ] Notifications and notification preferences (needs a Web Push backend and a VAPID key).
- [ ] Deep accessibility audit (axe already runs on every story and e2e journey) and 3D performance audit on a low-end phone.
- [ ] Not pilot ready yet: see `docs/web/pilot-readiness.md`.
