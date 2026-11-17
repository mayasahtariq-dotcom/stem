import type { Schemas } from "@mooshala/api-client";

/**
 * In-memory fake backend state behind the MSW handlers. Shapes follow the real
 * OpenAPI schemas, so the fakes can't drift from the contract silently
 * (TypeScript fails the build). Seeded so every screen state can be demoed.
 */
export interface FakeUser {
  id: string;
  phone: string | null;
  name: string | null;
  roles: Schemas["MeResponse"]["roles"];
  language_mode: Schemas["MeResponse"]["language_mode"];
  registered: boolean;
}

export interface FakeClassroom {
  id: string;
  name: string;
  subject: "BI" | "CH" | "PH";
  grade: number;
  join_key: string;
  teacher_name: string;
  student_count: number;
  allowed_language_modes: ("hindi" | "kannada")[];
  expired?: boolean;
}

export const DEMO = {
  studentPhone: "9876543210",
  teacherPhone: "9876500001",
  parentPhone: "9876500002",
  newPhone: "9123456780",
  validKey: "BI09-K4X2",
  expiredKey: "BI09-XPRD",
  kannadaOnlyKey: "CH10-KNDA",
  rateLimitedKey: "PH08-MANY",
} as const;

interface Db {
  users: Map<string, FakeUser>; // by token
  classrooms: FakeClassroom[];
  memberships: Map<string, Set<string>>; // userId -> classroom ids
  joinAttempts: Map<string, number>;
}

let db: Db;

const tokenForPhone = (tenDigits: string) => `fake-token-91${tenDigits}`;

export function resetFakeDb() {
  const classrooms: FakeClassroom[] = [
    { id: "c-bio9a", name: "Class 9-A Science", subject: "BI", grade: 9, join_key: DEMO.validKey, teacher_name: "Ravi Sir", student_count: 38, allowed_language_modes: ["hindi", "kannada"] },
    { id: "c-bio9c", name: "Class 9-C Science", subject: "BI", grade: 9, join_key: DEMO.expiredKey, teacher_name: "Ravi Sir", student_count: 12, allowed_language_modes: ["hindi", "kannada"], expired: true },
    { id: "c-chem10b", name: "Class 10-B Chemistry", subject: "CH", grade: 10, join_key: DEMO.kannadaOnlyKey, teacher_name: "Lakshmi Ma'am", student_count: 30, allowed_language_modes: ["kannada"] },
  ];
  const users = new Map<string, FakeUser>([
    [tokenForPhone(DEMO.studentPhone), { id: "u-asha", phone: `+91${DEMO.studentPhone}`, name: "Asha", roles: ["student"], language_mode: "hindi", registered: true }],
    [tokenForPhone(DEMO.teacherPhone), { id: "u-ravi", phone: `+91${DEMO.teacherPhone}`, name: "Ravi Sir", roles: ["teacher"], language_mode: "hindi", registered: true }],
    [tokenForPhone(DEMO.parentPhone), { id: "u-meena", phone: `+91${DEMO.parentPhone}`, name: "Meena", roles: ["parent"], language_mode: "kannada", registered: true }],
  ]);
  db = { users, classrooms, memberships: new Map([["u-asha", new Set(["c-bio9a"])]]), joinAttempts: new Map() };
}
resetFakeDb();

export const fakeDb = () => db;

/** Finds or creates the user behind a fake bearer token. */
export function userForToken(token: string): FakeUser {
  const existing = db.users.get(token);
  if (existing) return existing;
  const digits = token.replace(/\D/g, "");
  const user: FakeUser = {
    id: `u-${digits || token.slice(-8)}`,
    phone: token.startsWith("fake-token-") ? `+${digits}` : null,
    name: null,
    roles: [],
    language_mode: null,
    registered: false,
  };
  db.users.set(token, user);
  return user;
}
