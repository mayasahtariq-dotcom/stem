package com.stemtutor.core.data.course

import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.model.CourseDetail
import com.stemtutor.core.data.model.CourseSummary
import com.stemtutor.core.data.model.CourseUnit
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.LessonDetail
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.LessonSummary
import com.stemtutor.core.data.model.NewUpload
import com.stemtutor.core.data.model.PipelineStep
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.model.UploadProgress
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * In-memory notes → course for debug builds and tests. Each poll of [progress] after
 * [STEP_MS] moves the pipeline one step, so the generating screen shows real progress. The
 * generated course is a Class 9 Biology course on the cell with one flagged question, like the
 * backend's fake AI. Lesson 2 is generated when lesson 1 is opened (lazy, as in the real app).
 */
class FakeCourseRepository(
    private val auth: AuthRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : CourseRepository {
    private class Upload(val id: String, val owner: String, val personal: Boolean, val pages: Int, val started: Long) {
        var courseId: String? = null
    }

    private class Lesson(
        val id: String,
        val position: Int,
        val title: Tri,
        var status: GenerationStatus = GenerationStatus.NOT_STARTED,
        var published: Boolean = false,
        val questions: MutableList<LessonQuestion> = mutableListOf(),
    )

    private class Course(
        val id: String,
        val owner: String,
        var title: Tri,
        val personal: Boolean,
        val lessons: List<Lesson>,
        val assigned: MutableList<String> = mutableListOf(),
    )

    private val lock = Mutex()
    private val uploads = mutableMapOf<String, Upload>()
    private val courses = linkedMapOf<String, Course>()

    private fun fail(code: ApiErrorCode): Nothing = throw AppException(AppError.Api(code))

    private suspend fun me() = auth.currentSession()?.userId ?: fail(ApiErrorCode.UNAUTHORIZED)

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = runCatching { lock.withLock { block() } }

    override suspend fun createAndStart(upload: NewUpload) = guarded {
        if (upload.pages.isEmpty() || upload.pages.size > MAX_PAGES) fail(ApiErrorCode.INVALID_UPLOAD)
        val u = Upload(UUID.randomUUID().toString(), me(), upload.personal, upload.pages.size, clock())
        uploads[u.id] = u
        snapshot(u)
    }

    override suspend fun progress(uploadId: String) = guarded {
        val u = uploads[uploadId]?.takeIf { it.owner == me() } ?: fail(ApiErrorCode.NOT_FOUND)
        snapshot(u)
    }

    private fun snapshot(u: Upload): UploadProgress {
        val stepIndex = ((clock() - u.started) / STEP_MS).toInt().coerceIn(0, PipelineStep.entries.size - 1)
        val done = PipelineStep.entries.take(stepIndex + 1)
        val ready = done.last() == PipelineStep.READY
        if (ready && u.courseId == null) u.courseId = makeCourse(u).id
        val course = u.courseId?.let(courses::get)
        return UploadProgress(
            id = u.id,
            status = done.last().apiValue,
            stepsDone = done,
            pageCount = u.pages,
            courseId = course?.id,
            firstLessonId = course?.lessons?.firstOrNull()?.id,
        )
    }

    private fun makeCourse(u: Upload): Course {
        val lessons = SAMPLE_LESSON_TITLES.mapIndexed { i, t -> Lesson(UUID.randomUUID().toString(), i, t) }
        lessons[0].questions += sampleQuestions()
        lessons[0].status = GenerationStatus.READY
        if (u.personal) lessons.forEach { it.published = true }
        val course = Course(UUID.randomUUID().toString(), u.owner, Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ"), u.personal, lessons)
        courses[course.id] = course
        return course
    }

    private fun Course.summary() = CourseSummary(
        id = id,
        title = title,
        subject = Subject.BIOLOGY,
        grade = 9,
        status = if (personal || lessons.any { it.published }) "published" else "review",
        isPersonal = personal,
        aiMade = personal,
        lessonCount = lessons.size,
        publishedCount = lessons.count { it.published },
    )

    private fun Course.detail(forOwner: Boolean) = CourseDetail(
        summary = summary(),
        units = listOf(
            CourseUnit(
                "unit-1",
                Tri("Inside the cell", "कोशिका के अंदर", "ಜೀವಕೋಶದ ಒಳಗೆ"),
                lessons.filter { forOwner || it.published }.map { l ->
                    LessonSummary(l.id, l.position, l.title, l.status, l.published, l.questions.size, l.questions.count { it.flagged })
                },
            ),
        ),
        assignedClassroomIds = assigned.toList(),
        questionCount = lessons.sumOf { it.questions.size },
        editedCount = lessons.sumOf { l -> l.questions.count { it.teacherEdited } },
    )

    override suspend fun teacherCourses() = guarded {
        val id = me()
        courses.values.filter { it.owner == id }.reversed().map { it.summary() }
    }

    override suspend fun studentCourses() = guarded {
        val id = me()
        courses.values.filter { it.owner == id && it.personal }.map { it.summary() }
    }

    private suspend fun owned(courseId: String): Course =
        courses[courseId]?.takeIf { it.owner == me() } ?: fail(ApiErrorCode.NOT_FOUND)

    override suspend fun course(id: String) = guarded { owned(id).detail(forOwner = true) }

    override suspend fun renameCourse(id: String, title: Tri) = guarded {
        val c = owned(id)
        c.title = Tri(title.en.ifBlank { c.title.en }, title.hi.ifBlank { c.title.hi }, title.kn.ifBlank { c.title.kn })
        c.detail(forOwner = true)
    }

    private suspend fun findLesson(id: String): Pair<Course, Lesson> {
        val owner = me()
        for (c in courses.values) {
            val l = c.lessons.firstOrNull { it.id == id } ?: continue
            if (c.owner != owner) fail(ApiErrorCode.NOT_FOUND)
            return c to l
        }
        fail(ApiErrorCode.NOT_FOUND)
    }

    override suspend fun lesson(id: String) = guarded {
        val (c, l) = findLesson(id)
        // Opening lesson N generates lesson N+1, once (lazy generation).
        c.lessons.getOrNull(l.position + 1)?.takeIf { it.status == GenerationStatus.NOT_STARTED }?.let { next ->
            next.questions += sampleQuestions()
            next.status = GenerationStatus.READY
        }
        LessonDetail(l.id, c.id, l.title, l.status, l.published, l.questions.toList())
    }

    private suspend fun findQuestion(id: String): Pair<Lesson, Int> {
        val owner = me()
        for (c in courses.values) for (l in c.lessons) {
            val i = l.questions.indexOfFirst { it.id == id }
            if (i >= 0) return if (c.owner == owner) l to i else fail(ApiErrorCode.NOT_FOUND)
        }
        fail(ApiErrorCode.NOT_FOUND)
    }

    override suspend fun saveQuestion(question: LessonQuestion) = guarded {
        if (!question.prompt.isComplete()) fail(ApiErrorCode.INVALID_QUESTION)
        val (l, i) = findQuestion(question.id)
        val saved = question.copy(teacherEdited = true, flagReason = null, confidence = 1.0)
        l.questions[i] = saved
        saved
    }

    override suspend fun regenerateQuestion(id: String) = guarded {
        val (l, i) = findQuestion(id)
        val old = l.questions[i]
        val fresh = sampleQuestions()[1].copy(id = old.id, position = old.position)
        l.questions[i] = fresh
        fresh
    }

    override suspend fun deleteQuestion(id: String) = guarded {
        val (l, i) = findQuestion(id)
        l.questions.removeAt(i)
        Unit
    }

    override suspend fun publishLesson(lessonId: String, classroomId: String?) = guarded {
        val (c, l) = findLesson(lessonId)
        if (l.status != GenerationStatus.READY) fail(ApiErrorCode.LESSON_NOT_READY)
        l.published = true
        if (classroomId != null && classroomId !in c.assigned) c.assigned += classroomId
    }

    override suspend fun assignCourse(courseId: String, classroomIds: List<String>, dueAtIso: String?) = guarded {
        val c = owned(courseId)
        classroomIds.filter { it !in c.assigned }.forEach { c.assigned += it }
        c.detail(forOwner = true)
    }

    companion object {
        const val STEP_MS = 700L
        const val MAX_PAGES = 10

        private val SAMPLE_LESSON_TITLES = listOf(
            Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"),
            Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"),
            Tri("Energy in the cell", "कोशिका में ऊर्जा", "ಜೀವಕೋಶದಲ್ಲಿ ಶಕ್ತಿ"),
        )

        private val MITO = Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ")
        private val NUCLEUS = Tri("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್")
        private val RIBOSOME = Tri("Ribosome", "राइबोसोम", "ರೈಬೋಸೋಮ್")
        private val CHLORO = Tri("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್")
        private val WHY = Tri(
            "Mitochondria make energy from food, like a stove in the kitchen.",
            "माइटोकॉन्ड्रिया भोजन से ऊर्जा बनाता है, जैसे रसोई का चूल्हा।",
            "ಮೈಟೊಕಾಂಡ್ರಿಯಾ ಆಹಾರದಿಂದ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ, ಅಡುಗೆಮನೆಯ ಒಲೆಯಂತೆ.",
        )

        fun sampleQuestions(): List<LessonQuestion> {
            fun id() = UUID.randomUUID().toString()
            return listOf(
                LessonQuestion(
                    id(), 0, Tri("Which part of the cell makes energy?", "कोशिका का कौन सा भाग ऊर्जा बनाता है?", "ಜೀವಕೋಶದ ಯಾವ ಭಾಗ ಶಕ್ತಿ ಮಾಡುತ್ತದೆ?"),
                    WHY, QuestionBody.MultipleChoice(listOf(NUCLEUS, MITO, RIBOSOME), 1), 1, 0.9, null, false,
                ),
                LessonQuestion(
                    id(), 1, Tri("Which part controls the cell?", "कौन सा भाग कोशिका को नियंत्रित करता है?", "ಯಾವ ಭಾಗ ಜೀವಕೋಶವನ್ನು ನಿಯಂತ್ರಿಸುತ್ತದೆ?"),
                    WHY, QuestionBody.MultipleChoice(listOf(NUCLEUS, MITO, CHLORO), 0), 1, 0.9, null, false,
                ),
                LessonQuestion(
                    id(), 2, Tri("Which part is found only in plant cells?", "कौन सा भाग सिर्फ़ पादप कोशिका में होता है?", "ಯಾವ ಭಾಗ ಸಸ್ಯ ಕೋಶದಲ್ಲಿ ಮಾತ್ರ ಇದೆ?"),
                    WHY, QuestionBody.MultipleChoice(listOf(CHLORO, MITO, NUCLEUS), 0), 2, 0.55,
                    "Handwriting on page 2 is unclear: is this word 'chloroplast'?", false,
                ),
                LessonQuestion(
                    id(), 3, Tri("Match each part to its job.", "हर भाग को उसके काम से मिलाओ।", "ಪ್ರತಿ ಭಾಗವನ್ನು ಅದರ ಕೆಲಸಕ್ಕೆ ಹೊಂದಿಸಿ."),
                    WHY,
                    QuestionBody.MatchPairs(
                        listOf(
                            MITO to Tri("Makes energy", "ऊर्जा बनाता है", "ಶಕ್ತಿ ಮಾಡುತ್ತದೆ"),
                            NUCLEUS to Tri("Controls the cell", "कोशिका को नियंत्रित करता है", "ಜೀವಕೋಶ ನಿಯಂತ್ರಿಸುತ್ತದೆ"),
                            RIBOSOME to Tri("Makes proteins", "प्रोटीन बनाता है", "ಪ್ರೋಟೀನ್ ಮಾಡುತ್ತದೆ"),
                        ),
                    ),
                    1, 0.85, null, false,
                ),
                LessonQuestion(
                    id(), 4, Tri("Fill in the gas plants give out.", "पौधे जो गैस छोड़ते हैं, वह भरो।", "ಸಸ್ಯಗಳು ಬಿಡುವ ಅನಿಲವನ್ನು ತುಂಬಿ."),
                    WHY, QuestionBody.FillFormula("6CO₂ + 6H₂O → C₆H₁₂O₆ + 6 __", "O₂", listOf("O₂", "CO₂", "H₂")), 2, 0.8, null, false,
                ),
                LessonQuestion(
                    id(), 5, Tri("True or false?", "सही या गलत?", "ಸರಿ ಅಥವಾ ತಪ್ಪು?"),
                    WHY,
                    QuestionBody.TrueFalse(Tri("Plant cells have a cell wall.", "पादप कोशिका में कोशिका भित्ति होती है।", "ಸಸ್ಯ ಕೋಶದಲ್ಲಿ ಕೋಶ ಭಿತ್ತಿ ಇರುತ್ತದೆ."), true),
                    1, 0.9, null, false,
                ),
            )
        }
    }
}
