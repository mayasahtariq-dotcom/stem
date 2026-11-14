package com.stemtutor.feature.teacher.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.SpeechBubble
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
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

data class AssignUiState(
    val classrooms: List<TeacherClassroom> = emptyList(),
    val selected: Set<String> = emptySet(),
    /** Due date as UTC midnight, epoch millis (from the date picker); null = none. */
    val dueDateMillis: Long? = null,
    val saving: Boolean = false,
    val error: AppError? = null,
    val done: Boolean = false,
)

/** Assigns a course to classrooms with an optional due date. */
@HiltViewModel
class AssignViewModel @Inject constructor(
    private val courses: CourseRepository,
    classrooms: ClassroomRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val courseId: String = checkNotNull(savedState[COURSE_ID_ARG])
    private val _state = MutableStateFlow(AssignUiState())
    val state: StateFlow<AssignUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val list = classrooms.teacherClassrooms().getOrDefault(emptyList())
            val assigned = courses.course(courseId).getOrNull()?.assignedClassroomIds.orEmpty().toSet()
            _state.update { it.copy(classrooms = list, selected = assigned) }
        }
    }

    fun toggle(id: String) = _state.update { s -> s.copy(selected = if (id in s.selected) s.selected - id else s.selected + id) }
    fun setDue(millis: Long?) = _state.update { it.copy(dueDateMillis = millis) }

    fun save() {
        val s = _state.value
        if (s.selected.isEmpty() || s.saving) return
        _state.update { it.copy(saving = true, error = null) }
        val due = s.dueDateMillis?.let { DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it)) }
        viewModelScope.launch {
            courses.assignCourse(courseId, s.selected.toList(), due).fold(
                onSuccess = { _state.update { it.copy(saving = false, done = true) } },
                onFailure = { e -> _state.update { it.copy(saving = false, error = e.appError) } },
            )
        }
    }
}

@Composable
fun AssignRoute(onDone: () -> Unit, viewModel: AssignViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    AssignScreen(state, viewModel::toggle, viewModel::setDue, viewModel::save, onDone)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignScreen(
    state: AssignUiState,
    onToggle: (String) -> Unit,
    onDue: (Long?) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        TeacherHeader(bilingualStringResource(R.string.tc_assign))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.classrooms.isEmpty()) SpeechBubble(R.string.tc_create_class_first, Modifier.fillMaxWidth())
            state.classrooms.forEach { c ->
                ChoiceCard(c.id in state.selected, { onToggle(c.id) }) { Text(c.name, fontWeight = FontWeight.Bold, color = StemColors.Ink) }
            }
            BilingualText(R.string.tc_due_date, size = BilingualTextSize.Small)
            val due = state.dueDateMillis
            ChunkyButton(
                label = if (due == null) {
                    bilingualStringResource(R.string.tc_pick_date)
                } else {
                    val d = Instant.ofEpochMilli(due).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    com.stemtutor.core.designsystem.text.BilingualString(d, d)
                },
                onClick = { picking = true },
                modifier = Modifier.fillMaxWidth(),
                style = ChunkyButtonStyle.White,
            )
            if (due != null) ChunkyButton(R.string.tc_no_due, { onDue(null) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
            state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ChunkyButton(R.string.tc_assign, onSave, Modifier.fillMaxWidth(), enabled = state.selected.isNotEmpty() && !state.saving)
            ChunkyButton(DsR.string.ds_cancel, onCancel, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
    if (picking) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.dueDateMillis)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                ChunkyButton(DsR.string.ds_done, {
                    onDue(pickerState.selectedDateMillis)
                    picking = false
                })
            },
            dismissButton = { ChunkyButton(DsR.string.ds_cancel, { picking = false }, style = ChunkyButtonStyle.Ghost) },
        ) { DatePicker(pickerState) }
    }
}

@BilingualPreviews
@Composable
private fun AssignPreview() {
    StemTutorTheme {
        AssignScreen(
            AssignUiState(
                classrooms = listOf(TeacherClassroom("1", "Class 9-A Science", Subject.BIOLOGY, 9, "BI09-K4X2", LanguageMode.entries, 38)),
                selected = setOf("1"),
            ),
            {}, {}, {}, {},
        )
    }
}
