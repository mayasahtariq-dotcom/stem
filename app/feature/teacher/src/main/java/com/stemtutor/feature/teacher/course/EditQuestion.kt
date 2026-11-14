package com.stemtutor.feature.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.course.FakeCourseRepository
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.TeacherHeader
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.teacher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val QUESTION_ID_ARG = "questionId"

data class EditQuestionUiState(
    val loading: Boolean = true,
    val question: LessonQuestion? = null,
    val loadError: AppError? = null,
    val saveError: AppError? = null,
    val saving: Boolean = false,
    val saved: Boolean = false,
) {
    val valid: Boolean get() {
        val q = question ?: return false
        if (!q.prompt.isComplete() || !q.explanation.isComplete()) return false
        return when (val b = q.body) {
            is QuestionBody.MultipleChoice -> b.options.size >= 3 && b.options.all { it.isComplete() } && b.correctIndex in b.options.indices
            is QuestionBody.TrueFalse -> b.statement.isComplete()
            is QuestionBody.FillFormula -> "__" in b.formula && b.answer.isNotBlank() && b.answer in b.chips
            is QuestionBody.MatchPairs -> b.pairs.size >= 3 && b.pairs.all { it.first.isComplete() && it.second.isComplete() }
        }
    }
}

/** Edits one question in all three languages, including which option is correct. */
@HiltViewModel
class EditQuestionViewModel @Inject constructor(
    private val courses: CourseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val lessonId: String = checkNotNull(savedState[LESSON_ID_ARG])
    private val questionId: String = checkNotNull(savedState[QUESTION_ID_ARG])
    private val _state = MutableStateFlow(EditQuestionUiState())
    val state: StateFlow<EditQuestionUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            courses.lesson(lessonId).fold(
                onSuccess = { l ->
                    val q = l.questions.firstOrNull { it.id == questionId }
                    _state.update { it.copy(loading = false, question = q, loadError = if (q == null) AppError.Unknown else null) }
                },
                onFailure = { e -> _state.update { it.copy(loading = false, loadError = e.appError) } },
            )
        }
    }

    private fun edit(change: (LessonQuestion) -> LessonQuestion) = _state.update { s ->
        s.copy(question = s.question?.let(change), saveError = null)
    }

    fun setPrompt(t: Tri) = edit { it.copy(prompt = t) }
    fun setExplanation(t: Tri) = edit { it.copy(explanation = t) }
    fun setBody(b: QuestionBody) = edit { it.copy(body = b) }

    fun save() {
        val q = _state.value.question ?: return
        if (!_state.value.valid) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            courses.saveQuestion(q).fold(
                onSuccess = { _state.update { it.copy(saving = false, saved = true) } },
                onFailure = { e -> _state.update { it.copy(saving = false, saveError = e.appError) } },
            )
        }
    }
}

@Composable
fun EditQuestionRoute(onDone: () -> Unit, viewModel: EditQuestionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    EditQuestionScreen(state, viewModel::setPrompt, viewModel::setExplanation, viewModel::setBody, viewModel::save, onDone, viewModel::load)
}

@Composable
fun EditQuestionScreen(
    state: EditQuestionUiState,
    onPrompt: (Tri) -> Unit,
    onExplanation: (Tri) -> Unit,
    onBody: (QuestionBody) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    animate: Boolean = true,
) {
    val q = state.question
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground).imePadding()) {
        TeacherHeader(bilingualStringResource(R.string.tc_edit_question))
        when {
            state.loading -> LoadingScreen(animate = animate)
            q == null -> ErrorScreen(onRetry, messageRes = (state.loadError ?: AppError.Unknown).messageRes(), animate = animate)
            else -> {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TriEditor(R.string.tc_prompt, q.prompt, onPrompt)
                    BodyEditor(q.body, onBody)
                    TriEditor(R.string.tc_explanation, q.explanation, onExplanation)
                    state.saveError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
                    if (!state.valid) ErrorLine(bilingualStringResource(DsR.string.err_invalid_question))
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChunkyButton(DsR.string.ds_save, onSave, Modifier.fillMaxWidth(), enabled = state.valid && !state.saving)
                    ChunkyButton(DsR.string.ds_cancel, onCancel, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        }
    }
}

/** One field in English, Hindi and Kannada. */
@Composable
private fun TriEditor(label: Int?, value: Tri, onChange: (Tri) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (label != null) BilingualText(label, size = BilingualTextSize.Small)
        OutlinedTextField(value.hi, { onChange(value.copy(hi = it)) }, label = { Text("हिंदी") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value.kn, { onChange(value.copy(kn = it)) }, label = { Text("ಕನ್ನಡ") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value.en, { onChange(value.copy(en = it)) }, label = { Text("English") }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BodyEditor(body: QuestionBody, onBody: (QuestionBody) -> Unit) {
    when (body) {
        is QuestionBody.MultipleChoice -> body.options.forEachIndexed { i, option ->
            TriEditor(null, option) { t -> onBody(body.copy(options = body.options.toMutableList().also { it[i] = t })) }
            ChoiceCard(selected = body.correctIndex == i, onClick = { onBody(body.copy(correctIndex = i)) }) {
                BilingualText(bilingualStringResource(R.string.tc_option_n, i + 1), size = BilingualTextSize.Small)
                BilingualText(R.string.tc_mark_correct, size = BilingualTextSize.Small)
            }
        }
        is QuestionBody.TrueFalse -> {
            TriEditor(R.string.tc_statement, body.statement) { onBody(body.copy(statement = it)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceCard(body.answer, { onBody(body.copy(answer = true)) }, Modifier.weight(1f)) { BilingualText(R.string.tc_true) }
                ChoiceCard(!body.answer, { onBody(body.copy(answer = false)) }, Modifier.weight(1f)) { BilingualText(R.string.tc_false) }
            }
        }
        is QuestionBody.FillFormula -> Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val formula = bilingualStringResource(R.string.tc_formula)
            val answer = bilingualStringResource(R.string.tc_answer)
            val chips = bilingualStringResource(R.string.tc_chips)
            OutlinedTextField(body.formula, { onBody(body.copy(formula = it)) }, label = { Text("${formula.regional} · ${formula.english}") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(body.answer, { onBody(body.copy(answer = it.trim())) }, label = { Text("${answer.regional} · ${answer.english}") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                body.chips.joinToString(", "),
                { raw -> onBody(body.copy(chips = raw.split(",").map { it.trim() }.filter { it.isNotEmpty() })) },
                label = { Text("${chips.regional} · ${chips.english}") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is QuestionBody.MatchPairs -> body.pairs.forEachIndexed { i, (left, right) ->
            BilingualText(bilingualStringResource(R.string.tc_pair_n, i + 1), size = BilingualTextSize.Small)
            TriEditor(null, left) { t -> onBody(body.copy(pairs = body.pairs.toMutableList().also { it[i] = t to right })) }
            TriEditor(null, right) { t -> onBody(body.copy(pairs = body.pairs.toMutableList().also { it[i] = left to t })) }
        }
    }
}

@BilingualPreviews
@Composable
private fun EditQuestionPreview() {
    StemTutorTheme {
        EditQuestionScreen(
            EditQuestionUiState(loading = false, question = FakeCourseRepository.sampleQuestions().first()),
            {}, {}, {}, {}, {}, {}, animate = false,
        )
    }
}
