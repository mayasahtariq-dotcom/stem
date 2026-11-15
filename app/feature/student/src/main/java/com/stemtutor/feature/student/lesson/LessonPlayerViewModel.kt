package com.stemtutor.feature.student.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.audio.ListenController
import com.stemtutor.core.data.learning.Grading
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.Answer
import com.stemtutor.core.data.model.LessonDetail
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.LessonResult
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.StudentStats
import com.stemtutor.core.data.model.Term
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.core.network.appError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val LESSON_ARG = "lessonId"

/** What the feedback sheet shows after Check. */
data class Feedback(
    val correct: Boolean,
    val xp: Int,
    val heartLost: Boolean,
    /** For a wrong answer: the right one (null for match pairs, where every pair ends matched). */
    val correctAnswer: QuestionBody?,
    val explanation: Tri,
)

data class PlayerUiState(
    val loading: Boolean = true,
    val loadError: AppError? = null,
    val lesson: LessonDetail? = null,
    val terms: Map<String, Term> = emptyMap(),
    val stats: StudentStats = StudentStats.EMPTY,
    val queue: LessonQueue = LessonQueue(emptyList()),
    val choice: Answer? = null,
    val match: MatchState? = null,
    val feedback: Feedback? = null,
    val confirmClose: Boolean = false,
    val heartsEmpty: Boolean = false,
    val result: LessonResult? = null,
    val completing: Boolean = false,
    val completeError: AppError? = null,
    val notice: AppError? = null,
    val openTerm: Term? = null,
) {
    val question: LessonQuestion? get() = queue.current?.let { lesson?.questions?.getOrNull(it.questionIndex) }
    val retrying: Boolean get() = queue.current?.retry == true

    /** The answer ready to check: a choice, or a finished match. */
    val answer: Answer? get() = when (val body = question?.body) {
        is QuestionBody.MatchPairs -> match?.takeIf { it.done }?.let { Answer.Pairs(body.pairs.size, it.mistakes) }
        null -> null
        else -> choice
    }

    /** Check stays disabled until the student has answered (PRD). */
    val canCheck: Boolean get() = feedback == null && answer != null && !heartsEmpty

    val progress: Float get() {
        val total = queue.turns.size
        if (total == 0) return 0f
        return (queue.position + if (feedback != null) 1 else 0).toFloat() / total
    }
}

/**
 * Plays one lesson: one question per screen, Check, the feedback sheet, wrong answers asked
 * again at the end, hearts, then the end screen. Feedback is instant (graded on the phone);
 * the server grades again and its XP and hearts are the ones kept.
 */
@HiltViewModel
class LessonPlayerViewModel @Inject constructor(
    private val learning: LearningRepository,
    private val listen: ListenController,
    savedState: SavedStateHandle,
) : ViewModel() {
    val lessonId: String = checkNotNull(savedState[LESSON_ARG])
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()
    /** Replaceable in tests. Declared before init, which already uses it. */
    var clock: () -> Long = System::currentTimeMillis
    private var turnStartedAt = clock()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadError = null) }
        viewModelScope.launch {
            learning.startLesson(lessonId).fold(
                onSuccess = { p ->
                    val queue = LessonQueue.of(p.lesson.questions.size)
                    _state.value = PlayerUiState(
                        loading = false,
                        lesson = p.lesson,
                        terms = p.terms,
                        stats = p.stats,
                        queue = queue,
                        match = matchFor(p.lesson, queue),
                        heartsEmpty = p.stats.hearts <= 0,
                    )
                    turnStartedAt = clock()
                },
                onFailure = { e -> _state.update { it.copy(loading = false, loadError = e.appError) } },
            )
        }
    }

    private fun matchFor(lesson: LessonDetail, queue: LessonQueue): MatchState? {
        val q = queue.current?.let { lesson.questions.getOrNull(it.questionIndex) } ?: return null
        val body = q.body as? QuestionBody.MatchPairs ?: return null
        return MatchState.of(body.pairs.size, seed = q.id.hashCode() + queue.position)
    }

    fun choose(answer: Answer) {
        if (_state.value.feedback == null) _state.update { it.copy(choice = answer) }
    }

    fun tapLeft(pair: Int) = _state.update { s -> if (s.feedback != null) s else s.copy(match = s.match?.tapLeft(pair)) }

    fun tapRight(slot: Int) = _state.update { s -> if (s.feedback != null) s else s.copy(match = s.match?.tapRight(slot)) }

    fun check() {
        val s = _state.value
        if (!s.canCheck) return
        val q = s.question ?: return
        val answer = s.answer ?: return
        val correct = Grading.isCorrect(q.body, answer)
        val retry = s.retrying
        val heartLost = !correct && Grading.costsHeart(q.body)
        val xp = if (!correct) 0 else if (retry) XP_RETRY else XP_FIRST_TRY
        _state.update {
            it.copy(
                feedback = Feedback(correct, xp, heartLost, if (correct) null else q.body, q.explanation),
                queue = it.queue.answered(correct),
                stats = it.stats.copy(
                    hearts = if (heartLost) (it.stats.hearts - 1).coerceAtLeast(0) else it.stats.hearts,
                    xpTotal = it.stats.xpTotal + xp,
                    xpToday = it.stats.xpToday + xp,
                ),
            )
        }
        val elapsed = clock() - turnStartedAt
        viewModelScope.launch {
            learning.submitAnswer(q.id, answer, retry, elapsed).fold(
                onSuccess = { r -> _state.update { it.copy(stats = r.stats) } },
                onFailure = { e ->
                    val err = e.appError
                    if (err == AppError.Api(ApiErrorCode.HEARTS_EMPTY)) {
                        _state.update { it.copy(stats = it.stats.copy(hearts = 0)) }
                    }
                },
            )
        }
    }

    /** Continue after the feedback sheet: the next question, a pause at zero hearts, or the end. */
    fun next() {
        val s = _state.value
        if (s.feedback == null) return
        val queue = s.queue.next()
        val lesson = s.lesson ?: return
        if (queue.finished) {
            _state.update { it.copy(queue = queue, feedback = null, choice = null, match = null) }
            complete()
            return
        }
        _state.update {
            it.copy(
                queue = queue,
                feedback = null,
                choice = null,
                match = matchFor(lesson, queue),
                heartsEmpty = it.stats.hearts <= 0,
            )
        }
        turnStartedAt = clock()
    }

    fun complete() {
        _state.update { it.copy(completing = true, completeError = null) }
        viewModelScope.launch {
            learning.completeLesson(lessonId).fold(
                onSuccess = { r -> _state.update { it.copy(completing = false, result = r, stats = r.stats) } },
                onFailure = { e -> _state.update { it.copy(completing = false, completeError = e.appError) } },
            )
        }
    }

    /** Hearts may have refilled while the pause screen was open. */
    fun refreshHearts() {
        viewModelScope.launch {
            learning.stats().onSuccess { st -> _state.update { it.copy(stats = st, heartsEmpty = st.hearts <= 0) } }
        }
    }

    fun askClose(show: Boolean) = _state.update { it.copy(confirmClose = show) }

    fun listen(text: Tri, mode: LanguageMode, englishOnly: Boolean = false) {
        viewModelScope.launch {
            listen.speak(text, mode, englishOnly).onFailure { e -> _state.update { it.copy(notice = e.appError) } }
        }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    fun openTerm(key: String?) = _state.update { it.copy(openTerm = key?.let { k -> it.terms[k] }) }

    fun toggleSaved(term: Term) {
        val courseId = _state.value.lesson?.courseId ?: return
        val saved = !term.saved
        viewModelScope.launch {
            learning.setTermSaved(courseId, term.key, saved).onSuccess {
                _state.update { s ->
                    val updated = term.copy(saved = saved)
                    s.copy(terms = s.terms + (term.key to updated), openTerm = if (s.openTerm?.key == term.key) updated else s.openTerm)
                }
            }
        }
    }

    override fun onCleared() {
        listen.stop()
    }

    companion object {
        const val XP_FIRST_TRY = 10
        const val XP_RETRY = 5
    }
}
