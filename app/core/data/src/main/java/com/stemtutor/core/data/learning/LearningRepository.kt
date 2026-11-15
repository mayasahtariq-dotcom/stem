package com.stemtutor.core.data.learning

import com.stemtutor.core.data.course.QuestionCodec
import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.AttemptResult
import com.stemtutor.core.data.model.ContinueLesson
import com.stemtutor.core.data.model.CoursePath
import com.stemtutor.core.data.model.DoubtAnswer
import com.stemtutor.core.data.model.DoubtMode
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.HomeClass
import com.stemtutor.core.data.model.HomeCourse
import com.stemtutor.core.data.model.HomeSummary
import com.stemtutor.core.data.model.LessonDetail
import com.stemtutor.core.data.model.LessonResult
import com.stemtutor.core.data.model.NodeState
import com.stemtutor.core.data.model.PathNode
import com.stemtutor.core.data.model.PathUnit
import com.stemtutor.core.data.model.PlayableLesson
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.network.AttemptInDto
import com.stemtutor.core.network.DailyGoalDto
import com.stemtutor.core.network.DoubtInDto
import com.stemtutor.core.network.LearningApi
import com.stemtutor.core.network.PathDto
import com.stemtutor.core.network.StatsDto
import com.stemtutor.core.network.apiCall
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/** The student learning loop (PRD M3). Grading, XP, hearts and streaks happen on the server. */
interface LearningRepository {
    suspend fun home(): Result<HomeSummary>

    suspend fun stats(): Result<StudentStats>

    suspend fun setDailyGoal(goal: Int): Result<StudentStats>

    suspend fun path(courseId: String): Result<CoursePath>

    suspend fun startLesson(lessonId: String): Result<PlayableLesson>

    suspend fun submitAnswer(questionId: String, answer: Answer, retry: Boolean, timeMs: Long): Result<AttemptResult>

    suspend fun completeLesson(lessonId: String): Result<LessonResult>

    suspend fun terms(courseId: String): Result<List<Term>>

    suspend fun setTermSaved(courseId: String, key: String, saved: Boolean): Result<Unit>

    suspend fun askDoubt(
        courseId: String,
        text: String,
        mode: DoubtMode = DoubtMode.ASK,
        questionId: String? = null,
        termKey: String? = null,
    ): Result<DoubtAnswer>
}

internal fun StatsDto.toModel() = StudentStats(xpTotal, xpToday, dailyGoal, hearts, maxHearts, refillAt, streak, longestStreak)

internal fun PathDto.toModel(): CoursePath {
    var number = 0
    return CoursePath(
        courseId = courseId,
        title = Tri.of(title),
        subject = Subject.fromCode(subject),
        isPersonal = isPersonal,
        units = units.map { u ->
            PathUnit(
                u.id,
                Tri.of(u.title),
                u.lessons.map { l ->
                    number += 1
                    PathNode(
                        id = l.id,
                        number = number,
                        title = Tri.of(l.title),
                        state = when (l.state) {
                            "done" -> NodeState.DONE
                            "current" -> NodeState.CURRENT
                            else -> NodeState.LOCKED
                        },
                        generationStatus = GenerationStatus.fromApi(l.generationStatus),
                        questionCount = l.questionCount,
                        unlockedBy = l.unlockedBy?.let(Tri::of),
                    )
                },
            )
        },
        doneCount = doneCount,
        lessonCount = lessonCount,
        currentLessonId = currentLessonId,
    )
}

/** Term cards from the backend's JSON. */
object TermCodec {
    fun decode(key: String, content: JsonObject, sourcePage: Int, saved: Boolean = false) = Term(
        key = key,
        forms = tri(content["forms"]),
        transliteration = tri(content["transliteration"]),
        formal = tri(content["formal"]),
        meaning = tri(content["meaning"]),
        analogy = tri(content["analogy"]),
        related = (content["related"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty(),
        sourcePage = (content["source_page"] as? JsonPrimitive)?.intOrNull ?: sourcePage,
        saved = saved,
    )

    private fun tri(e: JsonElement?): Tri {
        val o = e as? JsonObject ?: return Tri.EMPTY
        fun s(k: String) = (o[k] as? JsonPrimitive)?.content.orEmpty()
        return Tri(s("en"), s("hi"), s("kn"))
    }
}

class RemoteLearningRepository(private val api: LearningApi) : LearningRepository {
    override suspend fun home() = apiCall {
        val dto = api.home()
        HomeSummary(
            name = dto.name,
            stats = dto.stats.toModel(),
            continueLesson = dto.continueLesson?.let {
                ContinueLesson(
                    it.courseId, Tri.of(it.courseTitle), Subject.fromCode(it.subject), it.lessonId, Tri.of(it.lessonTitle),
                    it.lessonNumber, it.lessonCount, GenerationStatus.fromApi(it.generationStatus), it.dueAt,
                )
            },
            classes = dto.classes.map { HomeClass(it.id, it.name, Subject.fromCode(it.subject), it.teacherName, it.newLessons) },
            courses = dto.courses.map {
                HomeCourse(it.id, Tri.of(it.title), Subject.fromCode(it.subject), it.isPersonal, it.aiMade, it.percentComplete, it.lessonCount)
            },
        )
    }

    override suspend fun stats() = apiCall { api.stats().toModel() }

    override suspend fun setDailyGoal(goal: Int) = apiCall { api.setDailyGoal(DailyGoalDto(goal)).toModel() }

    override suspend fun path(courseId: String) = apiCall { api.path(courseId).toModel() }

    override suspend fun startLesson(lessonId: String) = apiCall {
        val dto = api.startLesson(lessonId)
        val l = dto.lesson
        PlayableLesson(
            lesson = LessonDetail(
                id = l.id,
                courseId = l.courseId,
                title = Tri.of(l.title),
                generationStatus = GenerationStatus.fromApi(l.generationStatus),
                published = l.published,
                questions = l.questions.map { QuestionCodec.decode(it, l.terms) },
            ),
            terms = l.terms.mapValues { (key, content) -> TermCodec.decode(key, content, sourcePage = 1) },
            stats = dto.stats.toModel(),
        )
    }

    override suspend fun submitAnswer(questionId: String, answer: Answer, retry: Boolean, timeMs: Long) = apiCall {
        val dto = api.attempt(AttemptInDto(questionId, Grading.encode(answer), retry, timeMs))
        AttemptResult(dto.correct, dto.xpDelta, dto.heartLost, dto.stats.toModel())
    }

    override suspend fun completeLesson(lessonId: String) = apiCall {
        val dto = api.completeLesson(lessonId)
        LessonResult(dto.xpEarned, dto.accuracy, dto.streak, dto.streakExtended, dto.stats.toModel())
    }

    override suspend fun terms(courseId: String) = apiCall {
        api.terms(courseId).map { TermCodec.decode(it.key, it.content, it.sourcePage, it.saved) }
    }

    override suspend fun setTermSaved(courseId: String, key: String, saved: Boolean) = apiCall {
        if (saved) api.saveTerm(courseId, key) else api.unsaveTerm(courseId, key)
        Unit
    }

    override suspend fun askDoubt(courseId: String, text: String, mode: DoubtMode, questionId: String?, termKey: String?) = apiCall {
        val dto = api.doubt(DoubtInDto(courseId, questionId, termKey, text, mode.apiValue))
        DoubtAnswer(Tri.of(dto.answer), dto.inNotes, dto.sourcePages, dto.doubtsLeftToday)
    }
}
