package com.stemtutor.core.data.course

import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.auth.AuthSession
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.NewUpload
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.PageUpload
import com.stemtutor.core.data.model.PipelineStep
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.flaggedFirst
import com.stemtutor.core.network.QuestionDto
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private object Teacher : AuthRepository {
    override val session = flowOf(AuthSession("t1", "+919000000000"))
    override suspend fun currentSession() = AuthSession("t1", "+919000000000")
    override suspend fun sendOtp(phone: String) = Result.success(Unit)
    override suspend fun verifyOtp(phone: String, code: String) = Result.success(AuthSession("t1", phone))
    override suspend fun signOut() = Unit
    override suspend fun accessToken() = "t"
    override suspend fun refreshAccessToken() = "t"
}

class CourseFakesTest {
    private var now = 0L
    private val repo = FakeCourseRepository(Teacher) { now }
    private val upload = NewUpload(listOf(PageUpload(PageKind.TEXT, text = "notes")), null, null, "class-1", personal = false)

    @Test
    fun generatingStepsArriveInOrder() = runBlocking {
        val started = repo.createAndStart(upload).getOrThrow()
        val seen = mutableListOf(started.stepsDone.last())
        repeat(PipelineStep.entries.size) {
            now += FakeCourseRepository.STEP_MS
            val p = repo.progress(started.id).getOrThrow()
            if (p.stepsDone.last() != seen.last()) seen += p.stepsDone.last()
        }
        assertEquals(PipelineStep.entries.toList(), seen)
        val done = repo.progress(started.id).getOrThrow()
        assertTrue(done.ready && done.courseId != null && done.firstLessonId != null)
    }

    @Test
    fun lessonTwoIsMadeOnlyWhenLessonOneIsOpened() = runBlocking {
        val started = repo.createAndStart(upload).getOrThrow()
        now += FakeCourseRepository.STEP_MS * 10
        val done = repo.progress(started.id).getOrThrow()
        val course = repo.course(done.courseId!!).getOrThrow()
        assertEquals(listOf(GenerationStatus.READY, GenerationStatus.NOT_STARTED, GenerationStatus.NOT_STARTED), course.lessons.map { it.generationStatus })
        repo.lesson(done.firstLessonId!!).getOrThrow()
        val after = repo.course(done.courseId!!).getOrThrow()
        assertEquals(GenerationStatus.READY, after.lessons[1].generationStatus)
        assertEquals(GenerationStatus.NOT_STARTED, after.lessons[2].generationStatus)
    }

    @Test
    fun flaggedQuestionsComeFirstInReview() {
        val ordered = FakeCourseRepository.sampleQuestions().flaggedFirst()
        assertTrue(ordered.first().flagged)
        assertTrue(ordered.drop(1).none { it.flagged })
        assertEquals(ordered.drop(1).map { it.position }, ordered.drop(1).map { it.position }.sorted())
    }

    @Test
    fun editingClearsTheFlagAndMarksTeacherEdited() = runBlocking {
        val started = repo.createAndStart(upload).getOrThrow()
        now += FakeCourseRepository.STEP_MS * 10
        val lessonId = repo.progress(started.id).getOrThrow().firstLessonId!!
        val flagged = repo.lesson(lessonId).getOrThrow().questions.first { it.flagged }
        val saved = repo.saveQuestion(flagged.copy(prompt = flagged.prompt.copy(en = "Edited"))).getOrThrow()
        assertNull(saved.flagReason)
        assertTrue(saved.teacherEdited)
        assertEquals(1, repo.course(repo.progress(started.id).getOrThrow().courseId!!).getOrThrow().editedCount)
    }

    @Test
    fun codecResolvesTermKeysAndRoundTrips() {
        val terms = mapOf(
            "mitochondria" to buildJsonObject {
                put("forms", buildJsonObject { put("en", "Mitochondria"); put("hi", "माइटोकॉन्ड्रिया"); put("kn", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ") })
            },
        )
        fun tri(en: String) = buildJsonObject { put("en", en); put("hi", "$en-hi"); put("kn", "$en-kn") }
        val content = JsonObject(
            mapOf(
                "prompt" to tri("Which makes energy?"),
                "explanation" to tri("Because."),
                "options" to kotlinx.serialization.json.JsonArray(
                    listOf(
                        buildJsonObject { put("term_key", "mitochondria") },
                        buildJsonObject { put("text", tri("Nucleus")) },
                        buildJsonObject { put("text", tri("Ribosome")) },
                    ),
                ),
                "correct_index" to JsonPrimitive(0),
            ),
        )
        val q = QuestionCodec.decode(QuestionDto("q1", 0, "multiple_choice", content, 1, 0.9), terms)
        val body = q.body as QuestionBody.MultipleChoice
        assertEquals("माइटोकॉन्ड्रिया", body.options[0].hi)
        assertEquals("Nucleus", body.options[1].en)
        val encoded = QuestionCodec.encode(q)
        assertEquals("multiple_choice", (encoded["type"] as JsonPrimitive).content)
        val again = QuestionCodec.decode(QuestionDto("q1", 0, "multiple_choice", encoded, 1, 0.9), emptyMap())
        assertEquals(q.body, again.body)
        assertEquals(q.prompt, again.prompt)
    }
}
