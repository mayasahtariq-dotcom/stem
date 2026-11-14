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
- [ ] All tests pass, `progress.md` is ticked and the demo note is written.

## M3: Student learning

- [ ] The Continue card picks the right next lesson.
- [ ] All four question types work in both language modes.
- [ ] Wrong answers show the correct one and come back at the end of the lesson.
- [ ] Term cards open from underlined terms, and saving works.
- [ ] Doubt answers are bilingual and cite a notes page.
- [ ] Streak, XP, hearts and daily goal follow the rules.
- [ ] Listen works on every text, with instant playback in published lessons.
- [ ] A lesson loads in under 3 seconds on slow 4G and runs smoothly on a 2 GB phone.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

## M4: Teacher dashboard and Parent mode

- [ ] The teacher sees the tiles, weak topics and students list, with idle students in red.
- [ ] A student creates a code, and a parent links, approves and sees the dashboard.
- [ ] A parent with two children can switch between them.
- [ ] "Listen to summary" plays in the parent's language.
- [ ] Revoking consent and requesting deletion both work.
- [ ] The consent gate blocks lessons until approval or school consent.
- [ ] Tests prove parents can't see doubts or other students.
- [ ] Tests pass, `progress.md` is ticked and the demo note is written.

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
