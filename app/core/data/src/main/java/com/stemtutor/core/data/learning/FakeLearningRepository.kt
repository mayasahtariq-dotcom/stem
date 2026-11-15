package com.stemtutor.core.data.learning

import com.stemtutor.core.data.course.FakeCourseRepository
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
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.LessonResult
import com.stemtutor.core.data.model.NodeState
import com.stemtutor.core.data.model.PathNode
import com.stemtutor.core.data.model.PathUnit
import com.stemtutor.core.data.model.PlayableLesson
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The learning loop offline, for debug builds and tests: a built-in Class 9 Biology course
 * ("The Cell", three lessons using every question type) with XP, hearts and streaks kept in
 * memory by the same rules as the server.
 */
class FakeLearningRepository(
    private val name: String = "Asha",
    private val clock: () -> Long = System::currentTimeMillis,
) : LearningRepository {
    private class FakeLesson(val id: String, val title: Tri, val questions: List<LessonQuestion>, var done: Boolean = false)

    private val lock = Mutex()
    private val lessons = TITLES.mapIndexed { i, t ->
        FakeLesson("fake-lesson-${i + 1}", t, FakeCourseRepository.sampleQuestions().map { it.copy(flagReason = null) })
    }
    private val saved = mutableSetOf<String>()
    private var stats = StudentStats.EMPTY
    private var lastActiveDay: Long? = null
    private var heartsUpdatedAt = clock()
    private var doubtsToday = 0

    private fun fail(code: ApiErrorCode): Nothing = throw AppException(AppError.Api(code))

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = runCatching { lock.withLock { block() } }

    private fun day(ms: Long) = ms / DAY_MS

    private fun refill() {
        val s = stats
        if (s.hearts >= s.maxHearts) {
            heartsUpdatedAt = clock()
            return
        }
        val gained = ((clock() - heartsUpdatedAt) / REFILL_MS).toInt()
        if (gained > 0) {
            val hearts = (s.hearts + gained).coerceAtMost(s.maxHearts)
            heartsUpdatedAt = if (hearts >= s.maxHearts) clock() else heartsUpdatedAt + gained * REFILL_MS
            stats = s.copy(hearts = hearts)
        }
        val streak = if (lastActiveDay == null || day(clock()) - lastActiveDay!! > 1) 0 else stats.streak
        stats = stats.copy(streak = streak, refillAt = if (stats.hearts < stats.maxHearts) "${heartsUpdatedAt + REFILL_MS}" else null)
    }

    private fun current(): FakeLesson? = lessons.firstOrNull { !it.done }

    private fun path(): CoursePath {
        val cur = current()
        var previous: FakeLesson? = null
        val nodes = lessons.mapIndexed { i, l ->
            val state = when {
                l.done -> NodeState.DONE
                l == cur -> NodeState.CURRENT
                else -> NodeState.LOCKED
            }
            PathNode(l.id, i + 1, l.title, state, GenerationStatus.READY, l.questions.size, previous?.title?.takeIf { state == NodeState.LOCKED })
                .also { previous = l }
        }
        return CoursePath(
            COURSE_ID, COURSE_TITLE, Subject.BIOLOGY, isPersonal = false,
            units = listOf(PathUnit("fake-unit-1", UNIT_TITLE, nodes)),
            doneCount = lessons.count { it.done }, lessonCount = lessons.size, currentLessonId = cur?.id,
        )
    }

    override suspend fun home() = guarded {
        refill()
        val cur = current()
        HomeSummary(
            name = name,
            stats = stats,
            continueLesson = cur?.let {
                ContinueLesson(COURSE_ID, COURSE_TITLE, Subject.BIOLOGY, it.id, it.title, lessons.indexOf(it) + 1, lessons.size, GenerationStatus.READY)
            },
            classes = listOf(HomeClass("fake-class", "Class 9-A Science", Subject.BIOLOGY, "Mrs. Rao", lessons.count { !it.done })),
            courses = listOf(
                HomeCourse(COURSE_ID, COURSE_TITLE, Subject.BIOLOGY, false, false, 100 * lessons.count { it.done } / lessons.size, lessons.size),
            ),
        )
    }

    override suspend fun stats() = guarded {
        refill()
        stats
    }

    override suspend fun setDailyGoal(goal: Int) = guarded {
        if (goal !in StudentStats.DAILY_GOALS) fail(ApiErrorCode.INVALID_ANSWER)
        stats = stats.copy(dailyGoal = goal)
        stats
    }

    override suspend fun path(courseId: String) = guarded {
        if (courseId != COURSE_ID) fail(ApiErrorCode.NOT_FOUND)
        path()
    }

    private fun lesson(id: String) = lessons.firstOrNull { it.id == id } ?: fail(ApiErrorCode.NOT_FOUND)

    override suspend fun startLesson(lessonId: String) = guarded {
        refill()
        val l = lesson(lessonId)
        PlayableLesson(
            LessonDetail(l.id, COURSE_ID, l.title, GenerationStatus.READY, published = true, questions = l.questions),
            TERMS.associateBy { it.key }.mapValues { (k, t) -> t.copy(saved = k in saved) },
            stats,
        )
    }

    override suspend fun submitAnswer(questionId: String, answer: Answer, retry: Boolean, timeMs: Long) = guarded {
        refill()
        val q = lessons.flatMap { it.questions }.firstOrNull { it.id == questionId } ?: fail(ApiErrorCode.NOT_FOUND)
        val costs = Grading.costsHeart(q.body)
        if (stats.hearts <= 0 && costs) fail(ApiErrorCode.HEARTS_EMPTY)
        val correct = Grading.isCorrect(q.body, answer)
        val xp = if (!correct) 0 else if (retry) XP_RETRY else XP_FIRST_TRY
        val lost = !correct && costs
        if (lost) {
            if (stats.hearts >= stats.maxHearts) heartsUpdatedAt = clock()
            stats = stats.copy(hearts = stats.hearts - 1)
        }
        stats = stats.copy(xpTotal = stats.xpTotal + xp, xpToday = stats.xpToday + xp)
        refill()
        AttemptResult(correct, xp, lost, stats)
    }

    override suspend fun completeLesson(lessonId: String) = guarded {
        val l = lesson(lessonId)
        l.done = true
        val today = day(clock())
        val extended = lastActiveDay != today
        val streak = when {
            !extended -> stats.streak
            lastActiveDay == today - 1 -> stats.streak + 1
            else -> 1
        }
        lastActiveDay = today
        stats = stats.copy(
            xpTotal = stats.xpTotal + XP_BONUS,
            xpToday = stats.xpToday + XP_BONUS,
            streak = streak,
            longestStreak = maxOf(stats.longestStreak, streak),
        )
        LessonResult(XP_BONUS, 1.0, streak, extended, stats)
    }

    override suspend fun terms(courseId: String) = guarded {
        if (courseId != COURSE_ID) fail(ApiErrorCode.NOT_FOUND)
        TERMS.map { it.copy(saved = it.key in saved) }
    }

    override suspend fun setTermSaved(courseId: String, key: String, saved: Boolean) = guarded {
        if (TERMS.none { it.key == key }) fail(ApiErrorCode.NOT_FOUND)
        if (saved) this.saved += key else this.saved -= key
    }

    override suspend fun askDoubt(courseId: String, text: String, mode: DoubtMode, questionId: String?, termKey: String?) = guarded {
        if (doubtsToday >= DOUBTS_PER_DAY) fail(ApiErrorCode.DOUBT_LIMIT)
        doubtsToday += 1
        val left = DOUBTS_PER_DAY - doubtsToday
        if ("moon" in text.lowercase()) {
            DoubtAnswer(
                Tri(
                    "Your notes don't cover this, but the moon shines by reflecting sunlight.",
                    "आपके नोट्स में यह नहीं है, पर चाँद सूरज की रोशनी से चमकता है।",
                    "ನಿಮ್ಮ ನೋಟ್ಸ್‌ನಲ್ಲಿ ಇದು ಇಲ್ಲ, ಆದರೆ ಚಂದ್ರ ಸೂರ್ಯನ ಬೆಳಕಿನಿಂದ ಹೊಳೆಯುತ್ತಾನೆ.",
                ),
                inNotes = false, sourcePages = emptyList(), doubtsLeftToday = left,
            )
        } else {
            DoubtAnswer(
                if (mode == DoubtMode.SIMPLER) SIMPLER else ANSWER,
                inNotes = true, sourcePages = listOf(1), doubtsLeftToday = left,
            )
        }
    }

    companion object {
        const val COURSE_ID = "fake-course-cell"
        const val XP_FIRST_TRY = 10
        const val XP_RETRY = 5
        const val XP_BONUS = 20
        const val DOUBTS_PER_DAY = 20
        const val REFILL_MS = 4 * 60 * 60 * 1000L
        private const val DAY_MS = 24 * 60 * 60 * 1000L

        val COURSE_TITLE = Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ")
        private val UNIT_TITLE = Tri("Inside the cell", "कोशिका के अंदर", "ಜೀವಕೋಶದ ಒಳಗೆ")
        private val TITLES = listOf(
            Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"),
            Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"),
            Tri("Energy in the cell", "कोशिका में ऊर्जा", "ಜೀವಕೋಶದಲ್ಲಿ ಶಕ್ತಿ"),
        )
        private val ANSWER = Tri(
            "Mitochondria make energy for the cell, like a generator powers a village.",
            "माइटोकॉन्ड्रिया कोशिका के लिए ऊर्जा बनाते हैं, जैसे जनरेटर गाँव को बिजली देता है।",
            "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಜೀವಕೋಶಕ್ಕೆ ಶಕ್ತಿ ಮಾಡುತ್ತವೆ, ಜನರೇಟರ್ ಊರಿಗೆ ವಿದ್ಯುತ್ ಕೊಡುವಂತೆ.",
        )
        private val SIMPLER = Tri(
            "Mitochondria are the cell's stove: they turn food into energy.",
            "माइटोकॉन्ड्रिया कोशिका का चूल्हा है: यह खाने को ऊर्जा में बदलता है।",
            "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಜೀವಕೋಶದ ಒಲೆ: ಅದು ಆಹಾರವನ್ನು ಶಕ್ತಿಯಾಗಿ ಬದಲಾಯಿಸುತ್ತದೆ.",
        )

        private fun term(key: String, forms: Tri, translit: String, formal: Tri, meaning: Tri, analogy: Tri, related: List<String>) =
            Term(key, forms, Tri(translit, translit, translit), formal, meaning, analogy, related, sourcePage = 1)

        val TERMS = listOf(
            term(
                "mitochondria", Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ"), "maitokondriya",
                Tri("Mitochondrion", "सूत्रकणिका", "ತಂತುಕಣ"),
                Tri("The part of the cell that makes energy.", "कोशिका का वह भाग जो ऊर्जा बनाता है।", "ಶಕ್ತಿ ಮಾಡುವ ಜೀವಕೋಶದ ಭಾಗ."),
                Tri("Like a generator that powers a village.", "जैसे गाँव को बिजली देने वाला जनरेटर।", "ಊರಿಗೆ ವಿದ್ಯುತ್ ಕೊಡುವ ಜನರೇಟರ್‌ನಂತೆ."),
                listOf("nucleus"),
            ),
            term(
                "nucleus", Tri("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್"), "nyookliyas",
                Tri("Nucleus", "केंद्रक", "ಕೋಶಕೇಂದ್ರ"),
                Tri("The control centre of the cell.", "कोशिका का नियंत्रण केंद्र।", "ಜೀವಕೋಶದ ನಿಯಂತ್ರಣ ಕೇಂದ್ರ."),
                Tri("Like the head teacher of a school.", "जैसे स्कूल के प्रधानाध्यापक।", "ಶಾಲೆಯ ಮುಖ್ಯ ಶಿಕ್ಷಕರಂತೆ."),
                listOf("mitochondria"),
            ),
            term(
                "ribosome", Tri("Ribosome", "राइबोसोम", "ರೈಬೋಸೋಮ್"), "raibosom",
                Tri("Ribosome", "राइबोसोम", "ರೈಬೋಸೋಮ್"),
                Tri("Tiny parts that make proteins.", "छोटे भाग जो प्रोटीन बनाते हैं।", "ಪ್ರೋಟೀನ್ ಮಾಡುವ ಸಣ್ಣ ಭಾಗಗಳು."),
                Tri("Like cooks in a kitchen.", "जैसे रसोई के रसोइये।", "ಅಡುಗೆಮನೆಯ ಅಡುಗೆಯವರಂತೆ."),
                emptyList(),
            ),
            term(
                "chloroplast", Tri("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್"), "kloroplast",
                Tri("Chloroplast", "हरित लवक", "ಹರಿತ್ತು"),
                Tri("Green parts of plant cells that make food from sunlight.", "पादप कोशिका के हरे भाग जो धूप से भोजन बनाते हैं।", "ಸೂರ್ಯನ ಬೆಳಕಿನಿಂದ ಆಹಾರ ಮಾಡುವ ಸಸ್ಯ ಕೋಶದ ಹಸಿರು ಭಾಗಗಳು."),
                Tri("Like a solar cooker.", "जैसे सोलर कुकर।", "ಸೌರ ಕುಕ್ಕರ್‌ನಂತೆ."),
                listOf("cell_wall"),
            ),
            term(
                "cell_wall", Tri("Cell wall", "कोशिका भित्ति", "ಕೋಶ ಭಿತ್ತಿ"), "kosika bhitti",
                Tri("Cell wall", "कोशिका भित्ति", "ಕೋಶ ಭಿತ್ತಿ"),
                Tri("A stiff wall around plant cells.", "पादप कोशिका के चारों ओर मज़बूत दीवार।", "ಸಸ್ಯ ಕೋಶದ ಸುತ್ತ ಗಟ್ಟಿಯಾದ ಗೋಡೆ."),
                Tri("Like the boundary wall of a house.", "जैसे घर की चारदीवारी।", "ಮನೆಯ ಕಾಂಪೌಂಡ್ ಗೋಡೆಯಂತೆ."),
                listOf("chloroplast"),
            ),
        )
    }
}
