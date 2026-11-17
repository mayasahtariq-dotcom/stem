import type { Schemas } from "@mooshala/api-client";
import { fakeDb, onFakeDbReset, registerFakeStore } from "../fake-db";
import { buildLessons, COURSE_ID, COURSE_TITLE, UNITS, type FakeLesson, type Tri } from "../learning/course-data";
import { addFakeLessons, allFakeLessons, fakeLessons, fakeNow, indiaDay, learner } from "../learning/state";

/**
 * Teacher-side fake state: courses the teacher owns, uploads moving through the
 * AI pipeline steps (backend/app/ai/pipeline.py STEPS), the class roster and the
 * school-consent flag. Lessons themselves live in the learning store so an edit
 * a teacher makes is what students then play.
 */
export const UPLOAD_STEPS = ["queued", "reading", "chunking", "outlining", "terms", "lesson_1", "ready"] as const;
/** How long the fake pipeline spends on each step. */
export const STEP_MS = 600;
export const UPLOAD_LIMITS = { maxPages: 10, pdfBytes: 10 * 1024 * 1024, photoBytes: 12 * 1024 * 1024 };

const tri = (en: string, hi: string, kn: string): Tri => ({ en, hi, kn });

export interface FakeCourse {
  id: string;
  ownerId: string;
  title: Tri;
  subject: "BI" | "CH" | "PH";
  grade: number;
  classroomId: string | null;
  isPersonal: boolean;
  aiMade: boolean;
  status: "draft" | "published";
  units: { id: string; title: Tri }[];
  /** classroom id -> due date (ISO) or null */
  assigned: Record<string, string | null>;
  uploadId: string | null;
}

export interface FakeUpload {
  id: string;
  ownerId: string;
  pages: { kind: "photo" | "pdf" | "text"; stored: boolean }[];
  subject: "BI" | "CH" | "PH";
  grade: number;
  classroomId: string | null;
  personal: boolean;
  /** created until /start; then the step is derived from time */
  startedAt: number | null;
  /** a page whose text says "fail" makes the pipeline fail at "reading" */
  willFail: boolean;
  courseId: string | null;
}

export interface RosterStudent {
  id: string;
  name: string;
  joinedAt: string;
  /** Seeded stats for students who aren't real fake users. Asha's come from her learner state. */
  seed?: { streak: number; accuracy: number | null; idleDays: number; done: number; overdue: number };
}

interface TeacherState {
  courses: FakeCourse[];
  uploads: Map<string, FakeUpload>;
  roster: Record<string, RosterStudent[]>;
  schoolConsent: Record<string, boolean>;
  seq: number;
}

let st: TeacherState;

const ROSTER: [string, string, RosterStudent["seed"]][] = [
  ["u-asha", "Asha", undefined],
  ["s-bharat", "Bharat", { streak: 6, accuracy: 0.91, idleDays: 0, done: 4, overdue: 0 }],
  ["s-chitra", "Chitra", { streak: 2, accuracy: 0.78, idleDays: 1, done: 2, overdue: 0 }],
  ["s-dev", "Dev", { streak: 0, accuracy: 0.52, idleDays: 9, done: 1, overdue: 1 }],
  ["s-esha", "Esha", { streak: 4, accuracy: 0.84, idleDays: 0, done: 3, overdue: 0 }],
  ["s-farhan", "Farhan", { streak: 0, accuracy: null, idleDays: 14, done: 0, overdue: 1 }],
  ["s-gauri", "Gauri", { streak: 1, accuracy: 0.61, idleDays: 2, done: 1, overdue: 0 }],
  ["s-harsh", "Harsh", { streak: 3, accuracy: 0.7, idleDays: 0, done: 2, overdue: 0 }],
];

/** A draft course with flagged questions so review can be demoed without an upload. */
function draftLessons(courseId: string, prefix: string, titles: Tri[], flagged: boolean): FakeLesson[] {
  const source = buildLessons();
  return titles.map((title, i) => {
    const from = source[(i + 1) % 5]!;
    return {
      ...from,
      id: `${prefix}-l${i + 1}`,
      courseId,
      unit: 0,
      position: i + 1,
      title,
      generation_status: "ready" as const,
      published: false,
      questions: from.questions.map((q, j) => ({
        ...q,
        content: structuredClone(q.content),
        id: `${prefix}-q${i + 1}-${j + 1}`,
        confidence: flagged && j === 1 ? 0.41 : flagged && j === 3 ? 0.58 : 0.93,
        flag_reason: flagged && j === 1 ? "The source page is blurry here; check the answer." : flagged && j === 3 ? "Two options look correct." : null,
        teacher_edited: false,
      })),
    };
  });
}

onFakeDbReset(() => {
  st = {
    courses: [
      { id: COURSE_ID, ownerId: "u-ravi", title: COURSE_TITLE, subject: "BI", grade: 9, classroomId: "c-bio9a", isPersonal: false, aiMade: true, status: "published", units: UNITS, assigned: { "c-bio9a": null }, uploadId: null },
      { id: "course-tissues", ownerId: "u-ravi", title: tri("Tissues", "ऊतक", "ಅಂಗಾಂಶಗಳು"), subject: "BI", grade: 9, classroomId: "c-bio9a", isPersonal: false, aiMade: true, status: "draft", units: [{ id: "unit-tissues-1", title: tri("Plant tissues", "पादप ऊतक", "ಸಸ್ಯ ಅಂಗಾಂಶ") }], assigned: {}, uploadId: null },
    ],
    uploads: new Map(),
    roster: { "c-bio9a": ROSTER.map(([id, name, seed], i) => ({ id, name, seed, joinedAt: new Date(Date.UTC(2026, 10, 3 + i, 9)).toISOString() })), "c-bio9c": [] },
    schoolConsent: { "c-bio9a": true, "c-bio9c": false },
    seq: 1,
  };
  if (!allFakeLessons().some((l) => l.courseId === "course-tissues")) {
    addFakeLessons(draftLessons("course-tissues", "tis", [tri("What is a tissue?", "ऊतक क्या है?", "ಅಂಗಾಂಶ ಎಂದರೇನು?"), tri("Meristem", "विभज्योतक", "ವರ್ಧನ ಅಂಗಾಂಶ")], true));
  }
});

registerFakeStore("teacher", () => st, (v) => (st = v));

export const teacherState = () => st;
export const nextId = (p: string) => `${p}-${st.seq++}`;

export const courseById = (id: string) => st.courses.find((c) => c.id === id);
export const ownsClassroom = (teacherName: string | null, classroomId: string) =>
  fakeDb().classrooms.some((c) => c.id === classroomId && c.teacher_name === teacherName);

function lessonSummary(l: FakeLesson): Schemas["LessonSummary"] {
  return {
    id: l.id,
    position: l.position,
    title: l.title,
    generation_status: l.generation_status,
    published: l.published,
    question_count: l.questions.length,
    flagged_count: l.questions.filter((q) => q.flag_reason).length,
  };
}
export const lessonSummaryOut = lessonSummary;

export function courseSummary(c: FakeCourse): Schemas["CourseSummary"] {
  const ls = fakeLessons(c.id);
  return { id: c.id, title: c.title, subject: c.subject, grade: c.grade, status: c.status, is_personal: c.isPersonal, ai_made: c.aiMade, lesson_count: ls.length, published_count: ls.filter((l) => l.published).length };
}

export function courseOut(c: FakeCourse): Schemas["CourseOut"] {
  const ls = fakeLessons(c.id);
  return {
    ...courseSummary(c),
    units: c.units.map((u, i) => ({ id: u.id, title: u.title, lessons: ls.filter((l) => l.unit === i).map(lessonSummary) })).filter((u) => u.lessons.length),
    assigned_classroom_ids: Object.keys(c.assigned),
    question_count: ls.reduce((n, l) => n + l.questions.length, 0),
    edited_count: ls.reduce((n, l) => n + l.questions.filter((q) => q.teacher_edited).length, 0),
  };
}

/** Status of an upload; the step advances with the fake clock once started. */
export function uploadStatus(u: FakeUpload): Schemas["UploadStatus"] {
  const base = { id: u.id, page_count: u.pages.length, error: null as string | null, course_id: null as string | null, first_lesson_id: null as string | null };
  if (u.startedAt === null) return { ...base, status: "created", steps: [] };
  const idx = Math.min(UPLOAD_STEPS.length - 1, Math.floor((fakeNow() - u.startedAt) / STEP_MS));
  if (u.willFail && idx >= 1) return { ...base, status: "failed", steps: ["queued"], error: "No readable text found in the notes." };
  if (idx === UPLOAD_STEPS.length - 1 && !u.courseId) finishUpload(u);
  const done = UPLOAD_STEPS.slice(0, idx + 1);
  const status = UPLOAD_STEPS[idx]!;
  const course = u.courseId && idx >= 5 ? u.courseId : null;
  return { ...base, status, steps: [...done], course_id: course, first_lesson_id: course ? `${course}-l1` : null };
}

/** The pipeline's output: a draft course with a first lesson to review. */
function finishUpload(u: FakeUpload) {
  const id = nextId("course-up");
  const title = tri(`Uploaded notes ${st.seq - 1}`, `अपलोड किए नोट्स ${st.seq - 1}`, `ಅಪ್‌ಲೋಡ್ ಮಾಡಿದ ಟಿಪ್ಪಣಿ ${st.seq - 1}`);
  st.courses.push({ id, ownerId: u.ownerId, title, subject: u.subject, grade: u.grade, classroomId: u.classroomId, isPersonal: u.personal, aiMade: true, status: "draft", units: [{ id: `${id}-u1`, title: tri("Unit 1", "इकाई 1", "ಘಟಕ 1") }], assigned: {}, uploadId: u.id });
  addFakeLessons(draftLessons(id, id, [tri("Lesson 1 from your notes", "आपके नोट्स से पाठ 1", "ನಿಮ್ಮ ಟಿಪ್ಪಣಿಯಿಂದ ಪಾಠ 1")], true).map((l) => ({ ...l, id: `${id}-l1` })));
  u.courseId = id;
}

// ---- dashboard -------------------------------------------------------------------------------

const DAY = 86400_000;

function studentStats(s: RosterStudent) {
  if (s.seed) return s.seed;
  const l = learner(s.id);
  const firsts = l.attempts.filter((a) => a.firstTry);
  const done = [...l.progress.values()].filter((p) => p.completedAt).length;
  const lastAt = Math.max(0, ...[...l.progress.values()].map((p) => p.completedAt ?? p.startedAt), ...l.attempts.map((a) => a.at));
  const accFromBest = [...l.progress.values()].filter((p) => p.completedAt).map((p) => p.bestAccuracy);
  const accuracy = firsts.length ? firsts.filter((a) => a.correct).length / firsts.length : accFromBest.length ? accFromBest.reduce((a, b) => a + b, 0) / accFromBest.length : null;
  const idleDays = lastAt ? Math.max(0, Math.floor((Date.parse(indiaDay(fakeNow())) - Date.parse(indiaDay(lastAt))) / DAY)) : 30;
  return { streak: l.streak, accuracy, idleDays, done, overdue: 0 };
}

const LESSON_COUNT = () => fakeLessons(COURSE_ID).length;

export function studentRow(s: RosterStudent): Schemas["StudentRow"] {
  const x = studentStats(s);
  const current = fakeLessons(COURSE_ID)[Math.min(x.done, LESSON_COUNT() - 1)];
  return {
    id: s.id,
    name: s.name,
    streak: x.streak,
    current_lesson: current ? current.title : null,
    accuracy: x.accuracy === null ? null : Math.round(x.accuracy * 100) / 100,
    idle_days: x.idleDays,
    idle: x.idleDays >= 7,
    behind: x.overdue > 0 || (x.accuracy !== null && x.accuracy < 0.6),
    overdue: x.overdue,
  };
}

export function dashboard(classroomId: string): Schemas["DashboardOut"] {
  const c = fakeDb().classrooms.find((x) => x.id === classroomId)!;
  const students = (st.roster[classroomId] ?? []).map(studentRow);
  const withAcc = students.filter((s) => s.accuracy !== null);
  return {
    classroom_id: c.id,
    name: c.name,
    join_key: c.join_key,
    school_consent: st.schoolConsent[c.id] ?? false,
    courses: st.courses.filter((k) => classroomId in k.assigned).map((k) => ({ id: k.id, title: k.title })),
    tiles: {
      active_this_week: students.filter((s) => s.idle_days < 7).length,
      student_count: students.length,
      accuracy: withAcc.length ? Math.round((withAcc.reduce((n, s) => n + s.accuracy!, 0) / withAcc.length) * 100) / 100 : null,
      behind: students.filter((s) => s.behind).length,
    },
    weak_topics: students.length
      ? [
          { kind: "term", key: "mitochondria", name: tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"), accuracy: 0.48, attempts: 31, low_mastery_students: 4 },
          { kind: "term", key: "cell_wall", name: tri("Cell wall", "कोशिका भित्ति", "ಕೋಶ ಭಿತ್ತಿ"), accuracy: 0.57, attempts: 26, low_mastery_students: 3 },
          { kind: "formula", key: "photosynthesis", name: tri("Photosynthesis", "प्रकाश संश्लेषण", "ದ್ಯುತಿಸಂಶ್ಲೇಷಣೆ"), accuracy: 0.63, attempts: 18, low_mastery_students: 2 },
        ]
      : [],
    students,
  };
}

export function studentDetail(classroomId: string, studentId: string): Schemas["StudentDetailOut"] | null {
  const s = (st.roster[classroomId] ?? []).find((x) => x.id === studentId);
  if (!s) return null;
  const x = studentStats(s);
  const real = !s.seed ? learner(s.id) : null;
  const lessons = fakeLessons(COURSE_ID).map((l, i) => {
    const p = real?.progress.get(l.id);
    const completed = real ? Boolean(p?.completedAt) : i < x.done;
    return { lesson_id: l.id, title: l.title, completed, accuracy: completed ? (real ? (p?.bestAccuracy ?? null) : x.accuracy) : null };
  });
  return {
    id: s.id,
    name: s.name,
    streak: x.streak,
    last_active: x.idleDays >= 30 ? null : new Date(fakeNow() - x.idleDays * DAY).toISOString(),
    lessons_completed: x.done,
    lessons,
    weakest: [
      { kind: "term", name: tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"), mastery: 0.35 },
      { kind: "term", name: tri("Cell wall", "कोशिका भित्ति", "ಕೋಶ ಭಿತ್ತಿ"), mastery: 0.5 },
    ],
    confusions: x.done ? [{ expected: tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"), chosen: tri("Nucleus", "केंद्रक", "ಕೋಶಕೇಂದ್ರ"), count: 2 }] : [],
    trend: x.accuracy === null ? "steady" : x.accuracy >= 0.8 ? "improving" : x.accuracy < 0.6 ? "needs_practice" : "steady",
    usual_hour: x.done ? 18 : null,
  };
}
