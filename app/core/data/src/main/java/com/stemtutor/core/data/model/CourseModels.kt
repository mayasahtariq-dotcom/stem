package com.stemtutor.core.data.model

import com.stemtutor.core.common.language.LanguageMode

/** One text in English, Hindi and Kannada (course content is stored this way). */
data class Tri(val en: String, val hi: String, val kn: String) {
    fun regional(mode: LanguageMode): String = when (mode) {
        LanguageMode.HINDI -> hi
        LanguageMode.KANNADA -> kn
    }

    fun isComplete() = en.isNotBlank() && hi.isNotBlank() && kn.isNotBlank()

    companion object {
        val EMPTY = Tri("", "", "")
        fun of(map: Map<String, String>) = Tri(map["en"].orEmpty(), map["hi"].orEmpty(), map["kn"].orEmpty())
    }
}

enum class PageKind(val apiValue: String) { PHOTO("photo"), PDF("pdf"), TEXT("text") }

/** A page ready to send: compressed JPEG or PDF bytes, or typed text. */
class PageUpload(val kind: PageKind, val bytes: ByteArray? = null, val text: String? = null)

data class NewUpload(
    val pages: List<PageUpload>,
    val subject: Subject?,
    val grade: Int?,
    /** null with personal = false means a teacher course not tied to a class yet. */
    val classroomId: String?,
    val personal: Boolean,
)

/** The pipeline steps the generating screen shows, in order. */
enum class PipelineStep(val apiValue: String) {
    QUEUED("queued"),
    READING("reading"),
    CHUNKING("chunking"),
    OUTLINING("outlining"),
    TERMS("terms"),
    LESSON_1("lesson_1"),
    READY("ready"),
    ;

    companion object {
        fun fromApi(v: String) = entries.firstOrNull { it.apiValue == v }
    }
}

data class UploadProgress(
    val id: String,
    val status: String,
    val stepsDone: List<PipelineStep>,
    val pageCount: Int,
    val error: String? = null,
    val courseId: String? = null,
    val firstLessonId: String? = null,
) {
    val ready get() = status == "ready"
    val failed get() = status == "failed"
    val needsReview get() = status == "needs_review"
    val finished get() = ready || failed || needsReview
    /** The step in progress: the last one reported, until the pipeline finishes. */
    val current: PipelineStep? get() = if (finished) null else stepsDone.lastOrNull()
}

enum class GenerationStatus(val apiValue: String) {
    NOT_STARTED("not_started"),
    GENERATING("generating"),
    READY("ready"),
    FAILED("failed"),
    NEEDS_REVIEW("needs_review"),
    ;

    companion object {
        fun fromApi(v: String) = entries.firstOrNull { it.apiValue == v } ?: NOT_STARTED
    }
}

data class CourseSummary(
    val id: String,
    val title: Tri,
    val subject: Subject,
    val grade: Int,
    val status: String,
    val isPersonal: Boolean,
    val aiMade: Boolean,
    val lessonCount: Int,
    val publishedCount: Int,
)

data class LessonSummary(
    val id: String,
    val position: Int,
    val title: Tri,
    val generationStatus: GenerationStatus,
    val published: Boolean,
    val questionCount: Int,
    val flaggedCount: Int,
)

data class CourseUnit(val id: String, val title: Tri, val lessons: List<LessonSummary>)

data class CourseDetail(
    val summary: CourseSummary,
    val units: List<CourseUnit>,
    val assignedClassroomIds: List<String>,
    val questionCount: Int,
    val editedCount: Int,
) {
    val lessons get() = units.flatMap { it.lessons }
}

enum class QuestionType(val apiValue: String) {
    MULTIPLE_CHOICE("multiple_choice"),
    MATCH_PAIRS("match_pairs"),
    FILL_FORMULA("fill_formula"),
    TRUE_FALSE("true_false"),
    ;

    companion object {
        fun fromApi(v: String) = entries.firstOrNull { it.apiValue == v } ?: MULTIPLE_CHOICE
    }
}

sealed interface QuestionBody {
    val type: QuestionType

    data class MultipleChoice(val options: List<Tri>, val correctIndex: Int) : QuestionBody {
        override val type get() = QuestionType.MULTIPLE_CHOICE
    }

    data class MatchPairs(val pairs: List<Pair<Tri, Tri>>) : QuestionBody {
        override val type get() = QuestionType.MATCH_PAIRS
    }

    data class FillFormula(val formula: String, val answer: String, val chips: List<String>) : QuestionBody {
        override val type get() = QuestionType.FILL_FORMULA
    }

    data class TrueFalse(val statement: Tri, val answer: Boolean) : QuestionBody {
        override val type get() = QuestionType.TRUE_FALSE
    }
}

data class LessonQuestion(
    val id: String,
    val position: Int,
    val prompt: Tri,
    val explanation: Tri,
    val body: QuestionBody,
    val sourcePage: Int,
    val confidence: Double,
    val flagReason: String?,
    val teacherEdited: Boolean,
) {
    val flagged get() = flagReason != null
}

data class LessonDetail(
    val id: String,
    val courseId: String,
    val title: Tri,
    val generationStatus: GenerationStatus,
    val published: Boolean,
    val questions: List<LessonQuestion>,
)

/** Review order (PRD): questions the AI wasn't sure about come first, then by position. */
fun List<LessonQuestion>.flaggedFirst(): List<LessonQuestion> =
    sortedWith(compareBy<LessonQuestion> { !it.flagged }.thenBy { it.position })
