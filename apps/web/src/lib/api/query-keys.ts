/** Query key factory. Every server-state query key lives here. */
export const queryKeys = {
  me: ["me"] as const,
  myClassrooms: ["me", "classrooms"] as const,
  teacherClassrooms: ["classrooms"] as const,
  classroomPreview: (key: string) => ["classrooms", "preview", key] as const,
};
