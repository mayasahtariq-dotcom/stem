/** Query key factory. Every server-state query key lives here. */
export const queryKeys = {
  me: ["me"] as const,
  myClassrooms: ["me", "classrooms"] as const,
  teacherClassrooms: ["classrooms"] as const,
  classroomPreview: (key: string) => ["classrooms", "preview", key] as const,
  home: ["me", "home"] as const,
  stats: ["me", "stats"] as const,
  path: (courseId: string) => ["courses", courseId, "path"] as const,
  terms: (courseId: string) => ["courses", courseId, "terms"] as const,
  lesson: (lessonId: string) => ["lessons", lessonId] as const,
  game: {
    stats: ["game", "stats"] as const,
    shop: ["game", "shop"] as const,
    quests: ["game", "quests"] as const,
    league: ["game", "league"] as const,
    badges: ["game", "badges"] as const,
  },
};
