"use client";
import { useLocale } from "next-intl";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { toast } from "@/components/ui";
import type { TermMap } from "@/features/learning/content";
import { asApiError } from "@/lib/api/errors";
import { useBilingual } from "@/lib/i18n/use-bilingual";
import type { Tri } from "@/lib/i18n/use-tri";
import {
  useAssignCourse,
  useCourse,
  useCourses,
  useDashboard,
  usePublishLesson,
  useRegenerateQuestion,
  useRejectQuestion,
  useRenameCourse,
  useReviewLesson,
  useSaveQuestion,
  useSchoolConsent,
  useSelectedClass,
  useStudentDetail,
} from "./api";
import { CourseDetailScreen } from "./CourseDetailScreen";
import { CoursesScreen } from "./CoursesScreen";
import { LessonReviewScreen } from "./LessonReviewScreen";
import { OverviewScreen } from "./OverviewScreen";
import { optionLabels, publishBlock, reviewItems } from "./review";
import { StudentDetailScreen } from "./StudentDetailScreen";
import { StudentsScreen } from "./StudentsScreen";
import type { StudentFilter, StudentSort } from "./students";
import { kindForFile, type PageDraft, type UploadMeta } from "./upload";
import { UploadScreen } from "./UploadScreen";
import { useUpload } from "./use-upload";

const useErrorToast = () => {
  const errors = useBilingual("errors");
  return (e: unknown) => toast("error", errors(asApiError(e).code));
};

export function OverviewRoute() {
  const router = useRouter();
  const t = useBilingual("teacher");
  const onError = useErrorToast();
  const { classes, classId } = useSelectedClass();
  const dash = useDashboard(classId);
  const consent = useSchoolConsent(classId);
  return (
    <OverviewScreen
      data={dash.data}
      loading={classes.isLoading || dash.isLoading}
      error={classes.error ?? dash.error}
      noClasses={classes.data?.length === 0}
      consentSaving={consent.isPending}
      onToggleConsent={(enabled) => consent.mutate(enabled, { onSuccess: () => toast("success", t(enabled ? "consentSavedOn" : "consentSavedOff")), onError })}
      onOpenStudent={(id) => router.push(`/teacher/students/${id}`)}
      onRetry={() => void (classes.error ? classes.refetch() : dash.refetch())}
    />
  );
}

export function StudentsRoute() {
  const router = useRouter();
  const { classes, classId } = useSelectedClass();
  const dash = useDashboard(classId);
  const [filter, setFilter] = useState<StudentFilter>("all");
  const [sort, setSort] = useState<StudentSort>("name");
  const [query, setQuery] = useState("");
  return (
    <StudentsScreen
      rows={dash.data?.students}
      loading={classes.isLoading || dash.isLoading}
      error={classes.error ?? dash.error}
      noClasses={classes.data?.length === 0}
      filter={filter}
      sort={sort}
      query={query}
      onFilter={setFilter}
      onSort={setSort}
      onQuery={setQuery}
      onOpen={(id) => router.push(`/teacher/students/${id}`)}
      onRetry={() => void (classes.error ? classes.refetch() : dash.refetch())}
    />
  );
}

export function StudentDetailRoute({ studentId }: { studentId: string }) {
  const router = useRouter();
  const locale = useLocale();
  const { classes, classId } = useSelectedClass();
  const detail = useStudentDetail(classId, studentId);
  return (
    <StudentDetailScreen
      data={detail.data}
      loading={classes.isLoading || detail.isLoading}
      error={classes.error ?? detail.error}
      onBack={() => router.push("/teacher/students")}
      onRetry={() => void detail.refetch()}
      formatDate={(iso) => new Intl.DateTimeFormat(locale === "kn" ? "kn-IN" : "hi-IN", { day: "numeric", month: "short", timeZone: "Asia/Kolkata" }).format(new Date(iso))}
    />
  );
}

export function CoursesRoute() {
  const courses = useCourses();
  return <CoursesScreen courses={courses.data} loading={courses.isLoading} error={courses.error} onRetry={() => void courses.refetch()} />;
}

export function CourseDetailRoute({ courseId }: { courseId: string }) {
  const t = useBilingual("teacher");
  const onError = useErrorToast();
  const course = useCourse(courseId);
  const { classes, classId } = useSelectedClass();
  const publish = usePublishLesson(courseId);
  const assign = useAssignCourse(courseId);
  const rename = useRenameCourse(courseId);
  return (
    <CourseDetailScreen
      course={course.data}
      loading={course.isLoading}
      error={course.error}
      classes={classes.data ?? []}
      publishingId={publish.isPending ? (publish.variables?.lessonId ?? null) : null}
      assigning={assign.isPending}
      renaming={rename.isPending}
      onPublish={(lessonId) => {
        if (publish.isPending) return; // one publish at a time: no double taps
        publish.mutate({ lessonId, classroomId: course.data?.is_personal ? null : classId || null }, { onSuccess: () => toast("success", t("published")), onError });
      }}
      onAssign={(ids, dueAt) => assign.mutate({ classroom_ids: ids, due_at: dueAt }, { onSuccess: () => toast("success", t("assigned")), onError })}
      onRename={(en) => {
        const cur = (course.data?.title ?? {}) as Partial<Tri>;
        rename.mutate({ en, hi: cur.en === en ? (cur.hi ?? en) : en, kn: cur.en === en ? (cur.kn ?? en) : en }, { onError });
      }}
      onRetry={() => void course.refetch()}
    />
  );
}

export function LessonReviewRoute({ courseId, lessonId }: { courseId: string; lessonId: string }) {
  const t = useBilingual("teacher");
  const onError = useErrorToast();
  const lesson = useReviewLesson(lessonId);
  const { classId } = useSelectedClass();
  const course = useCourse(courseId);
  const save = useSaveQuestion(lessonId, courseId);
  const reject = useRejectQuestion(lessonId, courseId);
  const regen = useRegenerateQuestion(lessonId, courseId);
  const publish = usePublishLesson(courseId);
  const terms = (lesson.data?.terms ?? {}) as TermMap;
  const busyId = save.isPending ? save.variables.id : reject.isPending ? reject.variables : regen.isPending ? regen.variables : null;
  return (
    <LessonReviewScreen
      courseId={courseId}
      title={(lesson.data?.title as Tri | undefined) ?? null}
      items={lesson.data ? reviewItems(lesson.data) : undefined}
      optionLabels={(item) => optionLabels(item, terms)}
      published={Boolean(lesson.data?.published)}
      block={lesson.data ? publishBlock(lesson.data) : "notReady"}
      loading={lesson.isLoading}
      error={lesson.error}
      busyId={busyId}
      publishing={publish.isPending}
      onSave={async (item, question) => {
        try {
          await save.mutateAsync({ id: item.id, question });
          toast("success", t("questionSaved"));
          return true;
        } catch (e) {
          onError(e);
          return false;
        }
      }}
      onApprove={(item) => save.mutate({ id: item.id, question: item.content }, { onSuccess: () => toast("success", t("questionApproved")), onError })}
      onReject={(item) => reject.mutate(item.id, { onSuccess: () => toast("info", t("questionRejected")), onError })}
      onRegenerate={(item) => regen.mutate(item.id, { onSuccess: () => toast("success", t("questionRegenerated")), onError })}
      onPublish={() => {
        if (publish.isPending || lesson.data?.published) return;
        publish.mutate({ lessonId, classroomId: course.data?.is_personal ? null : classId || null }, { onSuccess: () => toast("success", t("published")), onError });
      }}
      onRetry={() => void lesson.refetch()}
    />
  );
}

let pageSeq = 0;

export function UploadRoute() {
  const t = useBilingual("teacher");
  const { classes, classId } = useSelectedClass();
  const upload = useUpload();
  const [pages, setPages] = useState<PageDraft[]>([]);
  const [typed, setTyped] = useState("");
  const [meta, setMeta] = useState<UploadMeta | null>(null);
  const current: UploadMeta = meta ?? { subject: classes.data?.find((c) => c.id === classId)?.subject ?? "BI", grade: classes.data?.find((c) => c.id === classId)?.grade ?? 9, classroomId: classId || null };
  return (
    <UploadScreen
      pages={pages}
      meta={current}
      classes={classes.data ?? []}
      typed={typed}
      run={upload.run}
      pollError={upload.pollError}
      onTyped={setTyped}
      onAddTyped={() => {
        setPages((x) => [...x, { key: `p${pageSeq++}`, kind: "text", name: `${t("typedPage").english}: ${typed.trim().slice(0, 40)}`, size: typed.length, text: typed }]);
        setTyped("");
      }}
      onFiles={(files) =>
        setPages((x) => [
          ...x,
          ...files.map((f) => {
            const kind = kindForFile(f);
            return { key: `p${pageSeq++}`, kind: kind ?? "photo", name: f.name, size: f.size, file: f, unsupported: !kind } satisfies PageDraft;
          }),
        ])
      }
      onRemove={(key) => setPages((x) => x.filter((p) => p.key !== key))}
      onMeta={setMeta}
      onSubmit={() => void upload.submit(pages, current)}
      onRetryPage={(i) => void upload.retryPage(i)}
      onRetryStart={() => void upload.retryStart()}
      onReset={() => {
        upload.reset();
        setPages([]);
      }}
    />
  );
}
