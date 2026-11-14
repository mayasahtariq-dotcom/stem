package com.stemtutor.feature.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.model.CourseDetail
import com.stemtutor.core.data.model.CourseSummary
import com.stemtutor.core.data.model.CourseUnit
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.LessonSummary
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
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

const val COURSE_ID_ARG = "courseId"

data class ReviewCourseUiState(
    val loading: Boolean = true,
    val course: CourseDetail? = null,
    val loadError: AppError? = null,
    val actionError: AppError? = null,
    val editingTitle: Boolean = false,
)

@HiltViewModel
class ReviewCourseViewModel @Inject constructor(
    private val courses: CourseRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val courseId: String = checkNotNull(savedState[COURSE_ID_ARG])
    private val _state = MutableStateFlow(ReviewCourseUiState())
    val state: StateFlow<ReviewCourseUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            courses.course(courseId).fold(
                onSuccess = { c -> _state.update { it.copy(loading = false, course = c, loadError = null) } },
                onFailure = { e -> _state.update { it.copy(loading = false, loadError = e.appError) } },
            )
        }
    }

    fun editTitle(open: Boolean) = _state.update { it.copy(editingTitle = open) }

    fun rename(title: Tri) {
        viewModelScope.launch {
            courses.renameCourse(courseId, title).fold(
                onSuccess = { c -> _state.update { it.copy(course = c, editingTitle = false, actionError = null) } },
                onFailure = { e -> _state.update { it.copy(actionError = e.appError, editingTitle = false) } },
            )
        }
    }
}

@Composable
fun ReviewCourseRoute(
    onOpenLesson: (String) -> Unit,
    onAssign: () -> Unit,
    onBack: () -> Unit,
    viewModel: ReviewCourseViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    ReviewCourseScreen(state, onOpenLesson, onAssign, onBack, viewModel::load, viewModel::editTitle, viewModel::rename)
}

@Composable
fun ReviewCourseScreen(
    state: ReviewCourseUiState,
    onOpenLesson: (String) -> Unit,
    onAssign: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onEditTitle: (Boolean) -> Unit,
    onRename: (Tri) -> Unit,
    animate: Boolean = true,
) {
    val course = state.course
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        TeacherHeader(course?.summary?.title?.bilingual() ?: bilingualStringResource(R.string.tc_courses))
        when {
            state.loading -> LoadingScreen(animate = animate)
            course == null -> ErrorScreen(onRetry, messageRes = (state.loadError ?: AppError.Unknown).messageRes(), animate = animate)
            else -> {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChunkyButton(R.string.tc_edit_title, { onEditTitle(true) }, Modifier.weight(1f), ChunkyButtonStyle.White)
                            ChunkyButton(R.string.tc_assign, onAssign, Modifier.weight(1f), ChunkyButtonStyle.White)
                        }
                    }
                    course.units.forEach { unit ->
                        item { BilingualText(unit.title.bilingual(), size = BilingualTextSize.Large) }
                        items(unit.lessons, key = { it.id }) { LessonCard(it) { onOpenLesson(it.id) } }
                    }
                    state.actionError?.let { item { ErrorLine(bilingualStringResource(it.messageRes())) } }
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        }
    }
    if (state.editingTitle && course != null) TitleDialog(course.summary.title, { onEditTitle(false) }, onRename)
}

@Composable
private fun LessonCard(lesson: LessonSummary, onClick: () -> Unit) {
    val questions = bilingualStringResource(R.string.tc_questions_n, lesson.questionCount)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StemColors.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${lesson.position + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = StemColors.TealBlue)
            BilingualText(lesson.title.bilingual(), modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val ready = lesson.published || lesson.generationStatus == GenerationStatus.READY
            Pill(
                lesson.generationStatus.labelRes(lesson.published),
                color = if (ready) StemColors.GreenTint else StemColors.Soft,
                text = if (ready) StemColors.CorrectText else StemColors.Ink,
            )
            if (lesson.questionCount > 0) Text("${questions.regional} · ${questions.english}", color = StemColors.Muted, fontSize = 13.sp)
            if (lesson.flaggedCount > 0) {
                val flagged = bilingualStringResource(R.string.tc_flagged_n, lesson.flaggedCount)
                Text("${flagged.regional} · ${flagged.english}", color = StemColors.FlagText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TitleDialog(current: Tri, onDismiss: () -> Unit, onSave: (Tri) -> Unit) {
    var en by remember { mutableStateOf(current.en) }
    var hi by remember { mutableStateOf(current.hi) }
    var kn by remember { mutableStateOf(current.kn) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { BilingualText(R.string.tc_edit_title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(en, { en = it }, label = { Text("English") }, singleLine = true)
                OutlinedTextField(hi, { hi = it }, label = { Text("हिंदी") }, singleLine = true)
                OutlinedTextField(kn, { kn = it }, label = { Text("ಕನ್ನಡ") }, singleLine = true)
            }
        },
        confirmButton = { ChunkyButton(DsR.string.ds_save, { onSave(Tri(en, hi, kn)) }, enabled = Tri(en, hi, kn).isComplete()) },
        dismissButton = { ChunkyButton(DsR.string.ds_cancel, onDismiss, style = ChunkyButtonStyle.Ghost) },
    )
}

@BilingualPreviews
@Composable
private fun ReviewCoursePreview() {
    val lessons = listOf(
        LessonSummary("1", 0, Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು"), GenerationStatus.READY, false, 6, 1),
        LessonSummary("2", 1, Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"), GenerationStatus.NOT_STARTED, false, 0, 0),
    )
    StemTutorTheme {
        ReviewCourseScreen(
            ReviewCourseUiState(
                loading = false,
                course = CourseDetail(
                    CourseSummary("c", Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ"), Subject.BIOLOGY, 9, "review", false, false, 2, 0),
                    listOf(CourseUnit("u", Tri("Inside the cell", "कोशिका के अंदर", "ಜೀವಕೋಶದ ಒಳಗೆ"), lessons)),
                    emptyList(), 6, 0,
                ),
            ),
            {}, {}, {}, {}, {}, {}, animate = false,
        )
    }
}
