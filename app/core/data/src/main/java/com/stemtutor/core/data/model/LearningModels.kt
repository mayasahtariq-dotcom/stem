package com.stemtutor.core.data.model

/** XP, hearts, streak and the daily goal. Calculated on the server; the app only shows them. */
data class StudentStats(
    val xpTotal: Int,
    val xpToday: Int,
    val dailyGoal: Int,
    val hearts: Int,
    val maxHearts: Int = 5,
    /** ISO time the next heart comes back, or null when hearts are full. */
    val refillAt: String? = null,
    val streak: Int,
    val longestStreak: Int = 0,
) {
    val goalProgress: Float get() = if (dailyGoal <= 0) 0f else (xpToday.toFloat() / dailyGoal).coerceIn(0f, 1f)

    companion object {
        val EMPTY = StudentStats(xpTotal = 0, xpToday = 0, dailyGoal = 50, hearts = 5, streak = 0)
        val DAILY_GOALS = listOf(30, 50, 100)
    }
}

data class ContinueLesson(
    val courseId: String,
    val courseTitle: Tri,
    val subject: Subject,
    val lessonId: String,
    val lessonTitle: Tri,
    val lessonNumber: Int,
    val lessonCount: Int,
    val generationStatus: GenerationStatus,
    val dueAt: String? = null,
)

data class HomeClass(val id: String, val name: String, val subject: Subject, val teacherName: String, val newLessons: Int)

data class HomeCourse(
    val id: String,
    val title: Tri,
    val subject: Subject,
    val isPersonal: Boolean,
    val aiMade: Boolean,
    val percentComplete: Int,
    val lessonCount: Int,
)

data class HomeSummary(
    val name: String,
    val stats: StudentStats,
    val continueLesson: ContinueLesson?,
    val classes: List<HomeClass>,
    val courses: List<HomeCourse>,
)

enum class NodeState { DONE, CURRENT, LOCKED }

data class PathNode(
    val id: String,
    val number: Int,
    val title: Tri,
    val state: NodeState,
    val generationStatus: GenerationStatus,
    val questionCount: Int,
    /** For a locked lesson: the lesson to finish first. */
    val unlockedBy: Tri? = null,
)

data class PathUnit(val id: String, val title: Tri, val lessons: List<PathNode>)

data class CoursePath(
    val courseId: String,
    val title: Tri,
    val subject: Subject,
    val isPersonal: Boolean,
    val units: List<PathUnit>,
    val doneCount: Int,
    val lessonCount: Int,
    val currentLessonId: String?,
) {
    val nodes: List<PathNode> get() = units.flatMap { it.lessons }
}

/** A term card: the word students hear in class, its formal translation, meaning and analogy. */
data class Term(
    val key: String,
    val forms: Tri,
    val transliteration: Tri,
    val formal: Tri,
    val meaning: Tri,
    val analogy: Tri,
    val related: List<String>,
    val sourcePage: Int,
    val saved: Boolean = false,
    /** The Student Twin's estimate of how well this student knows the term (0..1), if practised. */
    val mastery: Float? = null,
)

/** A lesson ready to play, with the course's term cards and the student's stats. */
data class PlayableLesson(
    val lesson: LessonDetail,
    val terms: Map<String, Term>,
    val stats: StudentStats,
)

/** What the student answered, in the shape the server grades. */
sealed interface Answer {
    data class Choice(val index: Int) : Answer
    data class TrueFalse(val value: Boolean) : Answer
    data class Chip(val chip: String) : Answer

    /** Match pairs are always finished by matching everything; [mistakes] wrong pairs along the way. */
    data class Pairs(val count: Int, val mistakes: Int) : Answer
}

data class AttemptResult(
    val correct: Boolean,
    val xpDelta: Int,
    val heartLost: Boolean,
    val stats: StudentStats,
)

data class LessonResult(
    val xpEarned: Int,
    val accuracy: Double,
    val streak: Int,
    val streakExtended: Boolean,
    val stats: StudentStats,
)

enum class DoubtMode(val apiValue: String) { ASK("ask"), SIMPLER("simpler"), EXAMPLE("example") }

data class DoubtAnswer(
    val answer: Tri,
    val inNotes: Boolean,
    val sourcePages: List<Int>,
    val doubtsLeftToday: Int,
)

/** Language codes the audio endpoint understands. */
enum class SpeechLanguage(val code: String) { HINDI("hi"), KANNADA("kn"), ENGLISH("en") }
