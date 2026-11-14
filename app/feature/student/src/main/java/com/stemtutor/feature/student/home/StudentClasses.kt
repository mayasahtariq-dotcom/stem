package com.stemtutor.feature.student.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.model.StudentClassroom
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.mascot.MeadowScene
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

sealed interface StudentClassesUiState {
    data object Loading : StudentClassesUiState
    data class Error(val error: AppError) : StudentClassesUiState
    data class Ready(val name: String, val classes: List<StudentClassroom>) : StudentClassesUiState
}

/** Shared by the Home and Class tabs: the student's name and the classes they've joined. */
@HiltViewModel
class StudentClassesViewModel @Inject constructor(
    private val classrooms: ClassroomRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<StudentClassesUiState>(StudentClassesUiState.Loading)
    val state: StateFlow<StudentClassesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        if (_state.value !is StudentClassesUiState.Ready) _state.value = StudentClassesUiState.Loading
        viewModelScope.launch {
            val name = profiles.me().getOrNull()?.name.orEmpty()
            classrooms.myClassrooms().fold(
                onSuccess = { _state.value = StudentClassesUiState.Ready(name, it) },
                onFailure = { _state.value = StudentClassesUiState.Error(it.appError) },
            )
        }
    }
}

/** Student home: the meadow and cow, a greeting, My classes and a Join button. */
@Composable
fun StudentHomeScreen(state: StudentClassesUiState, onJoin: () -> Unit, onRetry: () -> Unit, animate: Boolean = true) {
    when (state) {
        StudentClassesUiState.Loading -> LoadingScreen(animate = animate)
        is StudentClassesUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        is StudentClassesUiState.Ready -> LazyColumn(
            Modifier.fillMaxSize().background(StemColors.White),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                MeadowScene(animate = animate) {
                    CowMascot(
                        if (state.classes.isEmpty()) CowPose.Question else CowPose.Heart,
                        animate = animate,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                    )
                }
            }
            item {
                BilingualText(
                    text = bilingualStringResource(R.string.st_hello, state.name),
                    size = BilingualTextSize.Large,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            classList(state.classes, onJoin)
        }
    }
}

/** The Class tab: the full class list with the join box. */
@Composable
fun StudentClassTabScreen(state: StudentClassesUiState, onJoin: () -> Unit, onRetry: () -> Unit, animate: Boolean = true) {
    when (state) {
        StudentClassesUiState.Loading -> LoadingScreen(animate = animate)
        is StudentClassesUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        is StudentClassesUiState.Ready -> LazyColumn(
            Modifier.fillMaxSize().background(StemColors.Green),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            classList(state.classes, onJoin, onGreen = true)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.classList(
    classes: List<StudentClassroom>,
    onJoin: () -> Unit,
    onGreen: Boolean = false,
) {
    item {
        BilingualText(
            textRes = R.string.st_my_classes,
            size = BilingualTextSize.Large,
            regionalColor = if (onGreen) Color.White else StemColors.Ink,
            englishColor = if (onGreen) Color.White.copy(alpha = 0.85f) else StemColors.Muted,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
    if (classes.isEmpty()) {
        item { SpeechBubble(R.string.st_no_classes, Modifier.padding(horizontal = 20.dp).fillMaxWidth()) }
    }
    items(classes, key = { it.id }) { ClassCard(it) }
    item {
        ChunkyButton(
            R.string.st_join_class,
            onJoin,
            Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
            style = if (onGreen) ChunkyButtonStyle.White else ChunkyButtonStyle.Primary,
        )
    }
}

private fun Subject.icon() = when (this) {
    Subject.BIOLOGY -> Sticker.Sprout
    Subject.CHEMISTRY -> Sticker.Flask
    Subject.PHYSICS -> Sticker.Target
}

@Composable
private fun ClassCard(c: StudentClassroom) {
    val subject = bilingualStringResource(c.subject.nameRes())
    val grade = bilingualStringResource(DsR.string.grade_n, c.grade)
    val students = bilingualStringResource(DsR.string.students_n, c.studentCount)
    val teacher = bilingualStringResource(R.string.st_teacher_n, c.teacherName)
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(StemColors.Soft), contentAlignment = Alignment.Center) {
            StickerIcon(c.subject.icon(), size = 28.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(c.name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
            BilingualText(
                text = BilingualString(
                    "${subject.regional} · ${grade.regional} · ${teacher.regional}",
                    "${subject.english} · ${grade.english} · ${teacher.english} · ${students.english}",
                ),
                size = BilingualTextSize.Small,
            )
        }
    }
}

@BilingualPreviews
@Composable
private fun StudentHomePreview() {
    StemTutorTheme {
        StudentHomeScreen(
            StudentClassesUiState.Ready(
                "Ananya",
                listOf(StudentClassroom("1", "Class 9-A Science", "Mrs. Rao", Subject.BIOLOGY, 9, 38)),
            ),
            onJoin = {}, onRetry = {}, animate = false,
        )
    }
}
