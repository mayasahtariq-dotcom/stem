package com.stemtutor.feature.teacher

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.model.CourseSummary
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.component.TeacherHeader
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

@StringRes
fun Subject.nameRes(): Int = when (this) {
    Subject.BIOLOGY -> DsR.string.subject_biology
    Subject.CHEMISTRY -> DsR.string.subject_chemistry
    Subject.PHYSICS -> DsR.string.subject_physics
}

@StringRes
fun LanguageMode.nameRes(): Int = when (this) {
    LanguageMode.HINDI -> DsR.string.language_hindi
    LanguageMode.KANNADA -> DsR.string.language_kannada
}

fun Subject.icon() = when (this) {
    Subject.BIOLOGY -> Sticker.Sprout
    Subject.CHEMISTRY -> Sticker.Flask
    Subject.PHYSICS -> Sticker.Target
}

/** A small round icon button in the teacher header (e.g. to open Profile). */
@Composable
fun HeaderIconButton(icon: Sticker, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StemColors.White)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { StickerIcon(icon, size = 26.dp, contentDescription = description) }
}

// ---- Teacher home -----------------------------------------------------------------------

sealed interface TeacherHomeUiState {
    data object Loading : TeacherHomeUiState
    data class Error(val error: AppError) : TeacherHomeUiState
    data class Ready(
        val name: String,
        val classes: List<TeacherClassroom>,
        val courses: List<CourseSummary> = emptyList(),
    ) : TeacherHomeUiState
}

@HiltViewModel
class TeacherHomeViewModel @Inject constructor(
    private val classrooms: ClassroomRepository,
    private val profiles: ProfileRepository,
    private val courses: CourseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<TeacherHomeUiState>(TeacherHomeUiState.Loading)
    val state: StateFlow<TeacherHomeUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value !is TeacherHomeUiState.Ready) _state.value = TeacherHomeUiState.Loading
        viewModelScope.launch {
            val name = profiles.me().getOrNull()?.name.orEmpty()
            val myCourses = courses.teacherCourses().getOrDefault(emptyList())
            classrooms.teacherClassrooms().fold(
                onSuccess = { _state.value = TeacherHomeUiState.Ready(name, it, myCourses) },
                onFailure = { _state.value = TeacherHomeUiState.Error(it.appError) },
            )
        }
    }
}

@Composable
fun TeacherHomeRoute(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onProfile: () -> Unit,
    onMakeCourse: () -> Unit,
    onOpenCourse: (String) -> Unit,
    refreshKey: Int = 0,
    viewModel: TeacherHomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(refreshKey) { if (refreshKey > 0) viewModel.load() }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    TeacherHomeScreen(state, onCreate, onOpen, onProfile, viewModel::load, onMakeCourse = onMakeCourse, onOpenCourse = onOpenCourse)
}

@Composable
fun TeacherHomeScreen(
    state: TeacherHomeUiState,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onProfile: () -> Unit,
    onRetry: () -> Unit,
    animate: Boolean = true,
    onMakeCourse: () -> Unit = {},
    onOpenCourse: (String) -> Unit = {},
) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        val name = (state as? TeacherHomeUiState.Ready)?.name.orEmpty()
        TeacherHeader(
            title = bilingualStringResource(R.string.te_hello, name),
            actions = {
                HeaderIconButton(Sticker.Teacher, bilingualStringResource(DsR.string.ds_profile).regional, onProfile)
            },
        )
        when (state) {
            TeacherHomeUiState.Loading -> LoadingScreen(animate = animate)
            is TeacherHomeUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            is TeacherHomeUiState.Ready -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ChunkyButton(
                        com.stemtutor.feature.notes.R.string.nt_make_course,
                        onMakeCourse,
                        Modifier.fillMaxWidth(),
                        com.stemtutor.core.designsystem.component.ChunkyButtonStyle.Cream,
                    )
                }
                item { BilingualText(R.string.te_my_classes, size = BilingualTextSize.Large) }
                if (state.classes.isEmpty()) {
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            SpeechBubble(R.string.te_no_classes, Modifier.fillMaxWidth())
                            CowMascot(CowPose.Question, animate = animate)
                        }
                    }
                }
                items(state.classes, key = { it.id }) { ClassroomCard(it) { onOpen(it.id) } }
                item { ChunkyButton(R.string.te_create, onCreate, Modifier.fillMaxWidth()) }
                item { BilingualText(R.string.tc_courses, size = BilingualTextSize.Large) }
                if (state.courses.isEmpty()) item { SpeechBubble(R.string.tc_no_courses, Modifier.fillMaxWidth()) }
                items(state.courses, key = { "course-" + it.id }) { CourseCard(it) { onOpenCourse(it.id) } }
            }
        }
    }
}

@Composable
private fun CourseCard(c: CourseSummary, onClick: () -> Unit) {
    val counts = bilingualStringResource(R.string.tc_lessons_n, c.lessonCount, c.publishedCount)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StickerIcon(Sticker.Books, size = 36.dp)
        Column(Modifier.weight(1f)) {
            BilingualText(BilingualString(c.title.regional(currentLanguageMode()), c.title.en))
            Text("${counts.regional} · ${counts.english}", color = StemColors.Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ClassroomCard(c: TeacherClassroom, onClick: () -> Unit) {
    val subject = bilingualStringResource(c.subject.nameRes())
    val grade = bilingualStringResource(DsR.string.grade_n, c.grade)
    val students = bilingualStringResource(DsR.string.students_n, c.studentCount)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.White)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StickerIcon(c.subject.icon(), size = 36.dp)
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            BilingualText(
                BilingualString(
                    "${subject.regional} · ${grade.regional} · ${students.regional}",
                    "${subject.english} · ${grade.english} · ${students.english}",
                ),
                size = BilingualTextSize.Small,
            )
        }
        Text(c.joinKey, fontWeight = FontWeight.ExtraBold, color = StemColors.TealBlue, fontSize = 15.sp)
    }
}

@BilingualPreviews
@Composable
private fun TeacherHomePreview() {
    StemTutorTheme {
        TeacherHomeScreen(
            TeacherHomeUiState.Ready(
                "Mrs. Rao",
                listOf(TeacherClassroom("1", "Class 9-A Science", Subject.BIOLOGY, 9, "BI09-K4X2", LanguageMode.entries, 38)),
            ),
            {}, {}, {}, {}, animate = false,
        )
    }
}
