package com.stemtutor.feature.student.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.learning.LearningRepository
import com.stemtutor.core.data.model.ContinueLesson
import com.stemtutor.core.data.model.HomeCourse
import com.stemtutor.core.data.model.HomeSummary
import com.stemtutor.core.data.model.CourseSummary
import com.stemtutor.core.data.model.StudentClassroom
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.ProgressBar
import com.stemtutor.core.designsystem.component.TopStatsBar
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.mascot.MeadowScene
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.currentLanguageMode
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import com.stemtutor.feature.student.bilingual
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
    data class Ready(
        val name: String,
        val classes: List<StudentClassroom>,
        val courses: List<CourseSummary> = emptyList(),
        /** Stats, Continue, new-lesson counts and course progress (M3); null if it failed to load. */
        val home: HomeSummary? = null,
    ) : StudentClassesUiState
}

/** Shared by the Home and Class tabs: the student's name and the classes they've joined. */
@HiltViewModel
class StudentClassesViewModel @Inject constructor(
    private val classrooms: ClassroomRepository,
    private val profiles: ProfileRepository,
    private val courses: CourseRepository,
    private val learning: LearningRepository,
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
            val myCourses = courses.studentCourses().getOrDefault(emptyList())
            val home = learning.home().getOrNull()
            classrooms.myClassrooms().fold(
                onSuccess = { _state.value = StudentClassesUiState.Ready(name, it, myCourses, home) },
                onFailure = { _state.value = StudentClassesUiState.Error(it.appError) },
            )
        }
    }
}

/**
 * Student home (PRD M3): streak and stats, the meadow and cow, a greeting, the daily goal, the
 * Continue card, My classes (with new lessons), Make a course from notes, and My courses.
 */
@Composable
fun StudentHomeScreen(
    state: StudentClassesUiState,
    onJoin: () -> Unit,
    onRetry: () -> Unit,
    animate: Boolean = true,
    onMakeCourse: () -> Unit = {},
    onContinue: (lessonId: String) -> Unit = {},
    onOpenCourse: (courseId: String) -> Unit = {},
) {
    when (state) {
        StudentClassesUiState.Loading -> LoadingScreen(animate = animate)
        is StudentClassesUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        is StudentClassesUiState.Ready -> LazyColumn(
            Modifier.fillMaxSize().background(StemColors.White),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.home?.let { home ->
                item { TopStatsBar(streakDays = home.stats.streak, xp = home.stats.xpTotal, hearts = home.stats.hearts) }
            }
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
            state.home?.let { home ->
                item { DailyGoalCard(home) }
                home.continueLesson?.let { c -> item { ContinueCard(c) { onContinue(c.lessonId) } } }
                if (home.continueLesson == null && home.courses.isNotEmpty()) {
                    item { BilingualText(R.string.ln_all_done, modifier = Modifier.padding(horizontal = 20.dp)) }
                }
            }
            val newLessons = state.home?.classes?.associate { it.id to it.newLessons }.orEmpty()
            classList(state.classes, onJoin, newLessons = newLessons)
            item {
                ChunkyButton(
                    com.stemtutor.feature.notes.R.string.nt_make_course,
                    onMakeCourse,
                    Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    ChunkyButtonStyle.Cream,
                )
            }
            val homeCourses = state.home?.courses
            if (homeCourses != null && homeCourses.isNotEmpty()) {
                item {
                    BilingualText(R.string.st_my_courses, size = BilingualTextSize.Large, modifier = Modifier.padding(horizontal = 20.dp))
                }
                items(homeCourses, key = { "course-" + it.id }) { c -> ProgressCourseCard(c) { onOpenCourse(c.id) } }
            } else if (state.courses.isNotEmpty()) {
                item {
                    BilingualText(R.string.st_my_courses, size = BilingualTextSize.Large, modifier = Modifier.padding(horizontal = 20.dp))
                }
                items(state.courses, key = { "course-" + it.id }) { CourseCard(it) }
            }
        }
    }
}

/** Daily goal: today's XP against the goal (30, 50 or 100). */
@Composable
private fun DailyGoalCard(home: HomeSummary) {
    val stats = home.stats
    Column(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(StemColors.Soft).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StickerIcon(Sticker.Target, size = 28.dp)
            BilingualText(R.string.ln_daily_goal, modifier = Modifier.weight(1f))
            if (stats.streak > 0) {
                StickerIcon(Sticker.Flame, size = 22.dp)
                BilingualText(bilingualStringResource(R.string.ln_streak_days, stats.streak), size = BilingualTextSize.Small)
            }
        }
        ProgressBar(stats.goalProgress)
        BilingualText(
            if (stats.xpToday >= stats.dailyGoal) bilingualStringResource(R.string.ln_goal_met)
            else bilingualStringResource(R.string.ln_goal_progress, stats.xpToday, stats.dailyGoal),
            size = BilingualTextSize.Small,
        )
    }
}

/** The Continue card: the next unfinished lesson (assigned first, earliest due date first). */
@Composable
private fun ContinueCard(c: ContinueLesson, onStart: () -> Unit) {
    val mode = currentLanguageMode()
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.BlueTint)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BilingualText(R.string.ln_continue, size = BilingualTextSize.Small, regionalColor = StemColors.BlueDark, englishColor = StemColors.BlueDark)
        BilingualText(c.lessonTitle.bilingual(mode), size = BilingualTextSize.Large)
        BilingualText(
            BilingualString(
                "${c.courseTitle.regional(mode)} · ${bilingualStringResource(R.string.ln_lesson_of, c.lessonNumber, c.lessonCount).regional}",
                "${c.courseTitle.en} · ${bilingualStringResource(R.string.ln_lesson_of, c.lessonNumber, c.lessonCount).english}",
            ),
            size = BilingualTextSize.Small,
        )
        ChunkyButton(R.string.ln_start, onStart, Modifier.fillMaxWidth())
    }
}

/** A course with how much of it is done; tapping opens its lesson path. */
@Composable
private fun ProgressCourseCard(c: HomeCourse, onOpen: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.Cream)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StickerIcon(Sticker.Books, size = 32.dp)
            BilingualText(c.title.bilingual(currentLanguageMode()), modifier = Modifier.weight(1f))
            if (c.aiMade) {
                BilingualText(
                    R.string.st_ai_made,
                    size = BilingualTextSize.Small,
                    regionalColor = StemColors.Brown,
                    englishColor = StemColors.Brown,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StemColors.ChipFill).padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        ProgressBar(c.percentComplete / 100f)
        BilingualText(bilingualStringResource(R.string.ln_percent_done, c.percentComplete), size = BilingualTextSize.Small)
    }
}

/** A personal course before progress has loaded. Personal courses carry the "AI-made" badge. */
@Composable
private fun CourseCard(c: CourseSummary) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StemColors.Cream)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StickerIcon(Sticker.Books, size = 32.dp)
        BilingualText(BilingualString(c.title.regional(currentLanguageMode()), c.title.en), modifier = Modifier.weight(1f))
        if (c.aiMade) {
            BilingualText(
                R.string.st_ai_made,
                size = BilingualTextSize.Small,
                regionalColor = StemColors.Brown,
                englishColor = StemColors.Brown,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StemColors.ChipFill).padding(horizontal = 8.dp, vertical = 2.dp),
            )
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
    newLessons: Map<String, Int> = emptyMap(),
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
    items(classes, key = { it.id }) { ClassCard(it, newLessons[it.id] ?: 0) }
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
private fun ClassCard(c: StudentClassroom, newLessons: Int = 0) {
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
        if (newLessons > 0) {
            BilingualText(
                bilingualStringResource(R.string.ln_new_lessons, newLessons),
                size = BilingualTextSize.Small,
                regionalColor = StemColors.White,
                englishColor = StemColors.White,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StemColors.Blue).padding(horizontal = 8.dp, vertical = 2.dp),
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
                home = HomeSummary(
                    name = "Ananya",
                    stats = com.stemtutor.core.data.model.StudentStats(xpTotal = 120, xpToday = 30, dailyGoal = 50, hearts = 4, streak = 3),
                    continueLesson = ContinueLesson(
                        "c", com.stemtutor.core.data.model.Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ"), Subject.BIOLOGY, "l2",
                        com.stemtutor.core.data.model.Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ"),
                        2, 3, com.stemtutor.core.data.model.GenerationStatus.READY,
                    ),
                    classes = listOf(com.stemtutor.core.data.model.HomeClass("1", "Class 9-A Science", Subject.BIOLOGY, "Mrs. Rao", 2)),
                    courses = listOf(HomeCourse("c", com.stemtutor.core.data.model.Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ"), Subject.BIOLOGY, false, false, 33, 3)),
                ),
            ),
            onJoin = {}, onRetry = {}, animate = false,
        )
    }
}
