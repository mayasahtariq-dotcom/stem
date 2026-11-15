package com.stemtutor.core.data.learning

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.FakeAudioPlayer
import com.stemtutor.core.data.audio.FakeAudioRepository
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.NodeState
import com.stemtutor.core.data.model.SpeechLanguage
import com.stemtutor.core.data.model.Tri
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningFakesTest {
    private val word = Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ")

    @Test
    fun listenPlaysRegionalThenEnglish() = runTest {
        val audio = FakeAudioRepository()
        val player = FakeAudioPlayer()
        ListenController(audio, player).speak(word, LanguageMode.KANNADA)
        assertEquals(listOf("ಮೈಟೊಕಾಂಡ್ರಿಯಾ" to SpeechLanguage.KANNADA, "Mitochondria" to SpeechLanguage.ENGLISH), audio.requested)
        assertEquals(2, player.played.single().size)
    }

    @Test
    fun longPressPlaysOnlyEnglish() = runTest {
        val audio = FakeAudioRepository()
        ListenController(audio, FakeAudioPlayer()).speak(word, LanguageMode.HINDI, englishOnly = true)
        assertEquals(listOf("Mitochondria" to SpeechLanguage.ENGLISH), audio.requested)
    }

    @Test
    fun answersAreEncodedForTheServer() {
        assertEquals("1", Grading.encode(Answer.Choice(1))["index"]!!.jsonPrimitive.content)
        val pairs = Grading.encode(Answer.Pairs(3, mistakes = 2))
        assertEquals(3, pairs["pairs"]!!.jsonArray.size)
        assertEquals("2", pairs["mistakes"]!!.jsonPrimitive.content)
    }

    @Test
    fun fakeLessonPathUnlocksInOrder() = runTest {
        val repo = FakeLearningRepository()
        val path = repo.path(FakeLearningRepository.COURSE_ID).getOrThrow()
        assertEquals(listOf(NodeState.CURRENT, NodeState.LOCKED, NodeState.LOCKED), path.nodes.map { it.state })
        assertEquals(path.nodes[0].title, path.nodes[1].unlockedBy)
        repo.startLesson(path.nodes[0].id).getOrThrow()
        repo.completeLesson(path.nodes[0].id).getOrThrow()
        val after = repo.path(FakeLearningRepository.COURSE_ID).getOrThrow()
        assertEquals(listOf(NodeState.DONE, NodeState.CURRENT, NodeState.LOCKED), after.nodes.map { it.state })
        assertEquals(33, repo.home().getOrThrow().courses.single().percentComplete)
    }

    @Test
    fun fakeHeartsAndXpFollowTheRules() = runTest {
        var now = 0L
        val repo = FakeLearningRepository(clock = { now })
        val q = repo.startLesson("fake-lesson-1").getOrThrow().lesson.questions.first()
        val wrong = repo.submitAnswer(q.id, Answer.Choice(0), retry = false, timeMs = 0).getOrThrow()
        assertFalse(wrong.correct)
        assertEquals(4, wrong.stats.hearts)
        val retry = repo.submitAnswer(q.id, Answer.Choice(1), retry = true, timeMs = 0).getOrThrow()
        assertEquals(5, retry.xpDelta)
        now += FakeLearningRepository.REFILL_MS + 1
        assertEquals(5, repo.stats().getOrThrow().hearts)
    }

    @Test
    fun fakeDoubtsSayWhenNotInTheNotes() = runTest {
        val repo = FakeLearningRepository()
        assertTrue(repo.askDoubt(FakeLearningRepository.COURSE_ID, "What do mitochondria do?").getOrThrow().inNotes)
        val off = repo.askDoubt(FakeLearningRepository.COURSE_ID, "Why does the moon shine?").getOrThrow()
        assertFalse(off.inNotes)
        assertTrue(off.sourcePages.isEmpty())
    }
}
