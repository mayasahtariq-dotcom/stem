package com.stemtutor.core.data.model

// ---- teacher class dashboard (M4) ----

data class DashboardTiles(val activeThisWeek: Int, val studentCount: Int, val accuracy: Float?, val behind: Int)

data class WeakTopic(val kind: String, val name: Tri, val accuracy: Float, val attempts: Int, val lowMasteryStudents: Int)

data class StudentRow(
    val id: String,
    val name: String,
    val streak: Int,
    val currentLesson: Tri?,
    val accuracy: Float?,
    val idleDays: Int,
    val idle: Boolean,
    val behind: Boolean,
    val overdue: Int,
)

data class CourseRef(val id: String, val title: Tri)

data class ClassDashboard(
    val classroomId: String,
    val name: String,
    val joinKey: String,
    val schoolConsent: Boolean,
    val courses: List<CourseRef>,
    val tiles: DashboardTiles,
    val weakTopics: List<WeakTopic>,
    val students: List<StudentRow>,
)

enum class Trend(val apiValue: String) {
    IMPROVING("improving"),
    STEADY("steady"),
    NEEDS_PRACTICE("needs_practice"),
    ;

    companion object {
        fun fromApi(v: String) = entries.firstOrNull { it.apiValue == v } ?: STEADY
    }
}

data class LessonResultRow(val lessonId: String, val title: Tri, val completed: Boolean, val accuracy: Float?)

/** A concept from the Student Twin: a lesson or a term, with mastery 0..1. */
data class Concept(val kind: String, val name: Tri, val mastery: Float)

/** "Often mixes up [expected] and [chosen]". */
data class Confusion(val expected: Tri, val chosen: Tri, val count: Int)

data class StudentDetail(
    val id: String,
    val name: String,
    val streak: Int,
    val lastActive: String?,
    val lessonsCompleted: Int,
    val lessons: List<LessonResultRow>,
    val weakest: List<Concept>,
    val confusions: List<Confusion>,
    val trend: Trend,
)

// ---- parent links and consent ----

data class LinkCode(val code: String, val expiresAt: String)

data class LinkedParent(val id: String, val name: String, val phoneLast4: String, val consented: Boolean)

data class ChildClass(val id: String, val name: String, val teacherName: String)

data class ChildPreview(val id: String, val name: String, val classes: List<ChildClass>)

enum class ConsentStatus(val apiValue: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REVOKED("revoked"),
    ;

    companion object {
        fun fromApi(v: String) = entries.firstOrNull { it.apiValue == v } ?: PENDING
    }
}

data class Child(
    val id: String,
    val name: String,
    val consent: ConsentStatus,
    val consentGivenByMe: Boolean,
    val classes: List<ChildClass>,
)

// ---- parent dashboard ----

enum class Arrow { UP, DOWN, SAME }

data class WeekCard(val lessonsThisWeek: Int, val lessonsLastWeek: Int, val arrow: Arrow, val minutes: Int, val streak: Int)

data class AccuracyCard(val thisWeek: Float?, val lastWeek: Float?, val trend: Trend)

data class SubjectCard(val subject: Subject, val strongest: Tri?, val weakest: Tri?)

enum class AssignmentStatus { DONE, PENDING, OVERDUE }

data class Assignment(val courseId: String, val title: Tri, val classroom: String, val dueAt: String?, val status: AssignmentStatus)

data class ChildDashboard(
    val child: Child,
    val hasActivity: Boolean,
    val week: WeekCard,
    val accuracy: AccuracyCard,
    val subjects: List<SubjectCard>,
    val assignments: List<Assignment>,
    val classes: List<ChildClass>,
    val summary: Tri,
)
