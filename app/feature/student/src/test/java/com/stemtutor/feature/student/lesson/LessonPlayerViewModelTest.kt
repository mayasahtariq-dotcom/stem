package com.stemtutor.feature.student.lesson

import androidx.lifecycle.SavedStateHandle
import com.stemtutor.core.data.audio.FakeAudioPlayer
import com.stemtutor.core.data.audio.FakeAudioRepository
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.learning.FakeLearningRepository
import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.QuestionBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LessonPlayerViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeLearningRepository = FakeLearningRepository()) = LessonPlayerViewModel(
        repo,
        ListenController(FakeAudioRepository(), FakeAudioPlayer()),
        SavedStateHandle(mapOf(LESSON_ARG to "fake-lesson-1")),
    )

    private fun right(body: QuestionBody, pairsMistakes: Int = 0): Answer = when (body) {
        is QuestionBody.MultipleChoice -> Answer.Choice(body.correctIndex)
        is QuestionBody.TrueFalse -> Answer.TrueFalse(body.answer)
        is QuestionBody.FillFormula -> Answer.Chip(body.answer)
        is QuestionBody.MatchPairs -> Answer.Pairs(body.pairs.size, pairsMistakes)
    }

    /** Answers the current question (match pairs by tapping every pair correctly). */
    private fun answer(vm: LessonPlayerViewModel, correct: Boolean) {
        val body = vm.state.value.question!!.body
        if (body is QuestionBody.MatchPairs) {
            val order = vm.state.value.match!!.rightOrder
            for (pair in body.pairs.indices) {
                vm.tapLeft(pair)
                vm.tapRight(order.indexOf(pair))
            }
        } else if (correct) {
            vm.choose(right(body))
        } else {
            vm.choose(
                when (body) {
                    is QuestionBody.MultipleChoice -> Answer.Choice((body.correctIndex + 1) % body.options.size)
                    is QuestionBody.TrueFalse -> Answer.TrueFalse(!body.answer)
                    is QuestionBody.FillFormula -> Answer.Chip(body.chips.first { it != body.answer })
                    else -> error("unreachable")
                },
            )
        }
        vm.check()
    }

    @Test
    fun checkStaysDisabledUntilAnswered() {
        val vm = viewModel()
        assertFalse(vm.state.value.loading)
        assertFalse(vm.state.value.canCheck)
        vm.choose(Answer.Choice(0))
        assertTrue(vm.state.value.canCheck)
    }

    @Test
    fun rightAnswerShowsGreenFeedbackAndXp() {
        val vm = viewModel()
        answer(vm, correct = true)
        val f = vm.state.value.feedback!!
        assertTrue(f.correct)
        assertEquals(10, f.xp)
        assertEquals(10, vm.state.value.stats.xpTotal) // confirmed by the (fake) server
    }

    @Test
    fun wrongAnswerCostsAHeartAndComesBackAtTheEnd() {
        val vm = viewModel()
        answer(vm, correct = false)
        val f = vm.state.value.feedback!!
        assertFalse(f.correct)
        assertTrue(f.heartLost)
        assertNotNull(f.correctAnswer)
        assertEquals(4, vm.state.value.stats.hearts)
        assertEquals(7, vm.state.value.queue.turns.size) // six questions plus one retry
        vm.next()
        assertNull(vm.state.value.feedback)
        assertNull(vm.state.value.choice)
    }

    @Test
    fun playingThroughFinishesWithAResult() {
        val vm = viewModel()
        while (vm.state.value.result == null) {
            answer(vm, correct = true)
            vm.next()
        }
        val r = vm.state.value.result!!
        assertEquals(1, r.streak)
        assertTrue(r.streakExtended)
        assertEquals(6 * 10 + 20, vm.state.value.stats.xpTotal)
    }

    @Test
    fun outOfHeartsPausesTheLesson() {
        val vm = viewModel()
        repeat(5) {
            // Skip match pairs (it never costs a heart): answer it right and move on.
            if (vm.state.value.question!!.body is QuestionBody.MatchPairs) {
                answer(vm, correct = true)
                vm.next()
            }
            answer(vm, correct = false)
            vm.next()
        }
        assertEquals(0, vm.state.value.stats.hearts)
        assertTrue(vm.state.value.heartsEmpty)
        assertFalse(vm.state.value.canCheck)
    }

    @Test
    fun closingAsksFirst() {
        val vm = viewModel()
        vm.askClose(true)
        assertTrue(vm.state.value.confirmClose)
        vm.askClose(false)
        assertFalse(vm.state.value.confirmClose)
    }
}
