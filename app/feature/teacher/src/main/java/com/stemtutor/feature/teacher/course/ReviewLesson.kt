package com.stemtutor.feature.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.LessonDetail
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.QuestionBody
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.model.flaggedFirst
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.TeacherHeader
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.teacher.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val LESSON_ID_ARG = "lessonId"

data class ReviewLessonUiState(
    val loading: Boolean = true,
    val lesson: LessonDetail? = null,
    val loadError: AppError? = null,
    val actionError: AppError? = null,
    val busyQuestionId: String? = null,
    val confirmDeleteId: String? = null,
    val classrooms: List<TeacherClassroom> = emptyList(),
    val choosingClass: Boolean = false,
    val publishing: Boolean = false,
) {
    /** PRD: questions the AI wasn't confident about are listed first. */
    val ordered: List<LessonQuestion> get() = lesson?.questions?.flaggedFirst().orEmpty()
}

@HiltViewModel
class ReviewLessonViewModel @Inject constructor(
    private val courses: CourseRepository,
    private val classrooms: ClassroomRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val lessonId: String = checkNotNull(savedState[LESSON_ID_ARG])
    private val _state = MutableStateFlow(ReviewLessonUiState())
    val state: StateFlow<ReviewLessonUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { classrooms.teacherClassrooms().onSuccess { list -> _state.update { it.copy(classrooms = list) } } }
    }

    fun load() {
        viewModelScope.launch {
            courses.lesson(lessonId).fold(
                onSuccess = { l -> _state.update { it.copy(loading = false, lesson = l, loadError = null) } },
                onFailure = { e -> _state.update { it.copy(loading = false, loadError = e.appError) } },
            )
        }
    }

    /** While the lesson is still being made, check again every two seconds. */
    suspend fun pollWhileGenerating() {
        while (_state.value.lesson?.generationStatus.let { it == null || it == GenerationStatus.GENERATING || it == GenerationStatus.NOT_STARTED }) {
            delay(2_000)
            load()
        }
    }

    fun regenerate(id: String) = act(id) { courses.regenerateQuestion(id).map { } }

    fun askDelete(id: String?) = _state.update { it.copy(confirmDeleteId = id) }

    fun confirmDelete() {
        val id = _state.value.confirmDeleteId ?: return
        _state.update { it.copy(confirmDeleteId = null) }
        act(id) { courses.deleteQuestion(id) }
    }

    private fun act(id: String, block: suspend () -> Result<Unit>) {
        _state.update { it.copy(busyQuestionId = id, actionError = null) }
        viewModelScope.launch {
            block().fold(
                onSuccess = { load() },
                onFailure = { e -> _state.update { it.copy(actionError = e.appError) } },
            )
            _state.update { it.copy(busyQuestionId = null) }
        }
    }

    fun choosePublishClass(open: Boolean) = _state.update { it.copy(choosingClass = open) }

    fun publish(classroomId: String?) {
        _state.update { it.copy(choosingClass = false, publishing = true, actionError = null) }
        viewModelScope.launch {
            courses.publishLesson(lessonId, classroomId).fold(
                onSuccess = { load() },
                onFailure = { e -> _state.update { it.copy(actionError = e.appError) } },
            )
            _state.update { it.copy(publishing = false) }
        }
    }
}

@Composable
fun ReviewLessonRoute(onEdit: (questionId: String) -> Unit, onBack: () -> Unit, viewModel: ReviewLessonViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(state.lesson?.generationStatus) { viewModel.pollWhileGenerating() }
    ReviewLessonScreen(
        state = state,
        actions = ReviewLessonActions(
            onEdit = onEdit,
            onRedo = viewModel::regenerate,
            onDelete = { viewModel.askDelete(it) },
            onConfirmDelete = viewModel::confirmDelete,
            onCancelDelete = { viewModel.askDelete(null) },
            onPublish = {
                if (state.classrooms.size > 1) viewModel.choosePublishClass(true) else viewModel.publish(state.classrooms.firstOrNull()?.id)
            },
            onPublishTo = viewModel::publish,
            onCancelPublish = { viewModel.choosePublishClass(false) },
            onRetry = viewModel::load,
            onBack = onBack,
        ),
    )
}

class ReviewLessonActions(
    val onEdit: (String) -> Unit = {},
    val onRedo: (String) -> Unit = {},
    val onDelete: (String) -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onCancelDelete: () -> Unit = {},
    val onPublish: () -> Unit = {},
    val onPublishTo: (String?) -> Unit = {},
    val onCancelPublish: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@Composable
fun ReviewLessonScreen(state: ReviewLessonUiState, actions: ReviewLessonActions, animate: Boolean = true) {
    val lesson = state.lesson
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        TeacherHeader(lesson?.title?.bilingual() ?: BilingualString(" ", " "))
        when {
            state.loading -> LoadingScreen(animate = animate)
            lesson == null -> ErrorScreen(actions.onRetry, messageRes = (state.loadError ?: AppError.Unknown).messageRes(), animate = animate)
            lesson.generationStatus == GenerationStatus.GENERATING || lesson.generationStatus == GenerationStatus.NOT_STARTED ->
                LoadingScreen(messageRes = R.string.tc_lesson_generating, animate = animate)
            lesson.generationStatus == GenerationStatus.NEEDS_REVIEW || lesson.generationStatus == GenerationStatus.FAILED ->
                ErrorScreen(actions.onRetry, messageRes = R.string.tc_lesson_needs_review, animate = animate)
            else -> {
                LazyColumn(
                    Modifier.weight(1f).testTag("questions"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.ordered, key = { it.id }) { q ->
                        QuestionCard(q, busy = state.busyQuestionId == q.id, actions)
                    }
                    state.actionError?.let { item { ErrorLine(bilingualStringResource(it.messageRes())) } }
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lesson.published) {
                        Pill(R.string.tc_status_published, StemColors.GreenTint, StemColors.CorrectText)
                    } else {
                        ChunkyButton(R.string.tc_publish, actions.onPublish, Modifier.fillMaxWidth(), enabled = !state.publishing && lesson.questions.isNotEmpty())
                    }
                    ChunkyButton(DsR.string.ds_back, actions.onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        }
    }
    if (state.confirmDeleteId != null) {
        AlertDialog(
            onDismissRequest = actions.onCancelDelete,
            text = { BilingualText(R.string.tc_delete_confirm) },
            confirmButton = { ChunkyButton(R.string.tc_delete, actions.onConfirmDelete) },
            dismissButton = { ChunkyButton(DsR.string.ds_cancel, actions.onCancelDelete, style = ChunkyButtonStyle.Ghost) },
        )
    }
    if (state.choosingClass) {
        AlertDialog(
            onDismissRequest = actions.onCancelPublish,
            title = { BilingualText(R.string.tc_publish_to) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.classrooms.forEach { c ->
                        ChoiceCard(false, { actions.onPublishTo(c.id) }) { Text(c.name, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { ChunkyButton(DsR.string.ds_cancel, actions.onCancelPublish, style = ChunkyButtonStyle.Ghost) },
        )
    }
}

@Composable
private fun QuestionCard(q: LessonQuestion, busy: Boolean, actions: ReviewLessonActions) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StemColors.White)
            .padding(14.dp)
            .testTag(if (q.flagged) "question_flagged" else "question"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        q.flagReason?.let { reason ->
            // Yellow banner explaining why the AI wasn't sure (the reason is the AI's own words).
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(StemColors.FlagFill).padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StickerIcon(Sticker.Warn, size = 22.dp)
                Column {
                    BilingualText(R.string.tc_flag_banner, size = BilingualTextSize.Small, regionalColor = StemColors.FlagText, englishColor = StemColors.FlagText)
                    Text(reason, fontSize = 13.sp, color = StemColors.FlagText)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(q.body.type.labelRes())
            val page = bilingualStringResource(R.string.tc_page_n, q.sourcePage)
            Text("${page.regional} · ${page.english}", fontSize = 12.sp, color = StemColors.Muted)
        }
        BilingualText(q.prompt.bilingual())
        QuestionAnswers(q.body)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChunkyButton(R.string.tc_edit, { actions.onEdit(q.id) }, Modifier.weight(1f), ChunkyButtonStyle.White, enabled = !busy)
            ChunkyButton(R.string.tc_redo, { actions.onRedo(q.id) }, Modifier.weight(1f), ChunkyButtonStyle.White, enabled = !busy)
            ChunkyButton(R.string.tc_delete, { actions.onDelete(q.id) }, Modifier.weight(1f), ChunkyButtonStyle.Ghost, enabled = !busy)
        }
    }
}

@Composable
private fun QuestionAnswers(body: QuestionBody) {
    when (body) {
        is QuestionBody.MultipleChoice -> body.options.forEachIndexed { i, option -> AnswerLine(option.bilingual(), correct = i == body.correctIndex) }
        is QuestionBody.TrueFalse -> {
            BilingualText(body.statement.bilingual(), size = BilingualTextSize.Small)
            AnswerLine(bilingualStringResource(R.string.tc_true), correct = body.answer)
            AnswerLine(bilingualStringResource(R.string.tc_false), correct = !body.answer)
        }
        is QuestionBody.FillFormula -> {
            Text(body.formula, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            AnswerLine(bilingualStringResource(R.string.tc_answer_n, body.answer), correct = true)
        }
        is QuestionBody.MatchPairs -> body.pairs.forEach { (l, r) ->
            AnswerLine(BilingualString("${l.bilingual().regional} → ${r.bilingual().regional}", "${l.en} → ${r.en}"), correct = true)
        }
    }
}

@Composable
private fun AnswerLine(text: BilingualString, correct: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (correct) StemColors.GreenTint else StemColors.Soft)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(if (correct) "✓" else "•", color = if (correct) StemColors.CorrectText else StemColors.Muted, fontWeight = FontWeight.ExtraBold)
        BilingualText(text, size = BilingualTextSize.Small)
    }
}

@BilingualPreviews
@Composable
private fun ReviewLessonPreview() {
    StemTutorTheme {
        ReviewLessonScreen(
            ReviewLessonUiState(
                loading = false,
                lesson = LessonDetail(
                    "l", "c", Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"), GenerationStatus.READY, false,
                    com.stemtutor.core.data.course.FakeCourseRepository.sampleQuestions(),
                ),
            ),
            ReviewLessonActions(),
            animate = false,
        )
    }
}
