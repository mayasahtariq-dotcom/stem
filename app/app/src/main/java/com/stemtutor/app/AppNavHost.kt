package com.stemtutor.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.session.PendingJoinKey
import com.stemtutor.feature.notes.ReadyBanner
import com.stemtutor.feature.notes.capture.CaptureRoute
import com.stemtutor.feature.notes.crop.CropRotateScreen
import com.stemtutor.feature.notes.generating.GeneratingRoute
import com.stemtutor.feature.notes.generating.UPLOAD_ID_ARG
import com.stemtutor.feature.notes.upload.MAX_PAGES
import com.stemtutor.feature.notes.upload.NotesUploadRoute
import com.stemtutor.feature.notes.upload.NotesUploadViewModel
import com.stemtutor.feature.notes.upload.PERSONAL_ARG
import com.stemtutor.feature.teacher.course.AssignRoute
import com.stemtutor.feature.teacher.course.COURSE_ID_ARG
import com.stemtutor.feature.teacher.course.EditQuestionRoute
import com.stemtutor.feature.teacher.course.LESSON_ID_ARG
import com.stemtutor.feature.teacher.course.QUESTION_ID_ARG
import com.stemtutor.feature.teacher.course.ReviewCourseRoute
import com.stemtutor.feature.teacher.course.ReviewLessonRoute
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.choose.LanguageRoute
import com.stemtutor.feature.onboarding.choose.RoleRoute
import com.stemtutor.feature.onboarding.name.NameRoute
import com.stemtutor.feature.onboarding.otp.OtpRoute
import com.stemtutor.feature.onboarding.phone.PhoneRoute
import com.stemtutor.feature.onboarding.profile.ProfileRoute
import com.stemtutor.feature.onboarding.splash.SplashRoute
import com.stemtutor.feature.parent.AddChildRoute
import com.stemtutor.feature.parent.CHILD_ID_ARG
import com.stemtutor.feature.parent.ChildSettingsRoute
import com.stemtutor.feature.parent.ConsentRoute
import com.stemtutor.feature.parent.LinkCodes
import com.stemtutor.feature.parent.ParentShell
import com.stemtutor.core.data.model.ConsentStatus
import com.stemtutor.feature.student.StudentShell
import com.stemtutor.feature.student.doubt.DOUBT_QUESTION_ARG
import com.stemtutor.feature.student.doubt.DOUBT_START_ARG
import com.stemtutor.feature.student.doubt.DOUBT_TERM_ARG
import com.stemtutor.feature.student.doubt.DoubtRoute
import com.stemtutor.feature.student.family.LinkParentButton
import com.stemtutor.feature.student.family.LinkParentRoute
import com.stemtutor.feature.student.goal.DailyGoalSetting
import com.stemtutor.feature.student.learn.LessonPathRoute
import com.stemtutor.feature.student.lesson.DoubtStart
import com.stemtutor.feature.student.lesson.LESSON_ARG
import com.stemtutor.feature.student.lesson.LessonPlayerRoute
import com.stemtutor.feature.student.terms.GlossaryRoute
import com.stemtutor.feature.student.join.JOIN_KEY_ARG
import com.stemtutor.feature.student.join.JoinClassRoute
import com.stemtutor.feature.student.join.ScanQrRoute
import com.stemtutor.feature.teacher.TeacherHomeRoute
import com.stemtutor.feature.teacher.classroom.CLASSROOM_ID_ARG
import com.stemtutor.feature.teacher.classroom.ClassroomDetailRoute
import com.stemtutor.feature.teacher.classroom.ClassroomKeyRoute
import com.stemtutor.feature.teacher.classroom.ClassDashboardRoute
import com.stemtutor.feature.teacher.classroom.STUDENT_ID_ARG
import com.stemtutor.feature.teacher.classroom.StudentDetailRoute
import com.stemtutor.feature.teacher.create.CreateClassroomRoute

object Routes {
    const val SPLASH = "splash"
    const val PHONE = "phone"
    const val OTP = "otp"
    const val LANGUAGE = "language"
    const val ROLE = "role"
    const val NAME = "name"
    const val STUDENT = "student"
    const val STUDENT_JOIN = "student/join?$JOIN_KEY_ARG={$JOIN_KEY_ARG}"
    const val STUDENT_SCAN = "student/scan"
    const val STUDENT_PATH = "student/path/{courseId}"
    const val STUDENT_LESSON = "student/lesson/{$LESSON_ARG}"
    const val STUDENT_TERMS = "student/terms/{courseId}"
    const val STUDENT_DOUBT =
        "student/doubt/{courseId}?$DOUBT_QUESTION_ARG={$DOUBT_QUESTION_ARG}&$DOUBT_TERM_ARG={$DOUBT_TERM_ARG}&$DOUBT_START_ARG={$DOUBT_START_ARG}"
    const val TEACHER = "teacher"
    const val TEACHER_PROFILE = "teacher/profile"
    const val TEACHER_CREATE = "teacher/create"
    const val TEACHER_KEY = "teacher/classroom/{$CLASSROOM_ID_ARG}/key"
    const val TEACHER_CLASSROOM = "teacher/classroom/{$CLASSROOM_ID_ARG}"
    const val PARENT = "parent"
    const val PARENT_ADD = "parent/add"
    const val PARENT_SCAN = "parent/scan"
    const val PARENT_CONSENT = "parent/consent/{$CHILD_ID_ARG}"
    const val PARENT_CHILD = "parent/child/{$CHILD_ID_ARG}"
    const val STUDENT_LINK_PARENT = "student/link-parent"
    const val TEACHER_DASHBOARD = "teacher/classroom/{$CLASSROOM_ID_ARG}/dashboard"
    const val TEACHER_STUDENT = "teacher/classroom/{$CLASSROOM_ID_ARG}/student/{$STUDENT_ID_ARG}"
    const val NOTES_UPLOAD = "notes/upload?$PERSONAL_ARG={$PERSONAL_ARG}"
    const val NOTES_CAPTURE = "notes/capture"
    const val NOTES_CROP = "notes/crop/{pageId}"
    const val NOTES_GENERATING = "notes/generating/{$UPLOAD_ID_ARG}?teacher={teacher}"
    const val TEACHER_COURSE = "teacher/course/{$COURSE_ID_ARG}"
    const val TEACHER_ASSIGN = "teacher/course/{$COURSE_ID_ARG}/assign"
    const val TEACHER_LESSON = "teacher/lesson/{$LESSON_ID_ARG}"
    const val TEACHER_QUESTION = "teacher/lesson/{$LESSON_ID_ARG}/question/{$QUESTION_ID_ARG}"

    fun studentPath(courseId: String) = "student/path/$courseId"
    fun studentLesson(lessonId: String) = "student/lesson/$lessonId"
    fun studentTerms(courseId: String) = "student/terms/$courseId"
    fun studentDoubt(courseId: String, questionId: String?, termKey: String?, start: DoubtStart): String {
        val startArg = when (start) {
            DoubtStart.NONE -> ""
            DoubtStart.WHY -> "why"
            DoubtStart.SIMPLER -> "simpler"
        }
        return "student/doubt/$courseId?$DOUBT_QUESTION_ARG=${questionId.orEmpty()}&$DOUBT_TERM_ARG=${termKey.orEmpty()}&$DOUBT_START_ARG=$startArg"
    }
    fun join(key: String? = null) = if (key == null) "student/join" else "student/join?$JOIN_KEY_ARG=$key"
    fun notesUpload(personal: Boolean) = "notes/upload?$PERSONAL_ARG=$personal"
    fun notesCrop(pageId: String) = "notes/crop/$pageId"
    fun notesGenerating(uploadId: String, teacher: Boolean) = "notes/generating/$uploadId?teacher=$teacher"
    fun teacherCourse(id: String) = "teacher/course/$id"
    fun teacherAssign(id: String) = "teacher/course/$id/assign"
    fun teacherLesson(id: String) = "teacher/lesson/$id"
    fun teacherQuestion(lessonId: String, questionId: String) = "teacher/lesson/$lessonId/question/$questionId"
    fun parentConsent(childId: String) = "parent/consent/$childId"
    fun parentChild(childId: String) = "parent/child/$childId"
    fun teacherDashboard(id: String) = "teacher/classroom/$id/dashboard"
    fun teacherStudent(classroomId: String, studentId: String) = "teacher/classroom/$classroomId/student/$studentId"
    fun teacherKey(id: String) = "teacher/classroom/$id/key"
    fun teacherClassroom(id: String) = "teacher/classroom/$id"
    fun home(role: Role) = when (role) {
        Role.STUDENT -> STUDENT
        Role.TEACHER -> TEACHER
        Role.PARENT -> PARENT
    }
}

private const val SCANNED_KEY = "scanned_key"
private const val REFRESH = "refresh"

/** Onboarding first, then one section per role (student, teacher, parent). */
@Composable
fun AppNavHost(
    pendingJoinKey: PendingJoinKey,
    watcher: GenerationWatcher,
    navController: NavHostController = rememberNavController(),
) {
    val onboarding: (OnboardingNav) -> Unit = { navController.goTo(it) }
    val notice by watcher.notice.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashRoute(onboarding) }
        composable(Routes.PHONE) { PhoneRoute(onboarding) }
        composable(Routes.OTP) { OtpRoute(onboarding) }
        composable(Routes.LANGUAGE) { LanguageRoute(onboarding) }
        composable(Routes.ROLE) { RoleRoute(onboarding) }
        composable(Routes.NAME) { NameRoute(onboarding) }

        composable(Routes.STUDENT) { entry ->
            val refresh by entry.savedStateHandle.getStateFlow(REFRESH, 0).collectAsStateWithLifecycle()
            val pending by pendingJoinKey.key.collectAsStateWithLifecycle()
            LaunchedEffect(pending) {
                pendingJoinKey.consume()?.let { navController.navigate(Routes.join(it)) }
            }
            StudentShell(
                onJoin = { navController.navigate(Routes.join()) },
                onMakeCourse = { navController.navigate(Routes.notesUpload(personal = true)) },
                onPlayLesson = { navController.navigate(Routes.studentLesson(it)) },
                onOpenCourse = { navController.navigate(Routes.studentPath(it)) },
                onOpenTerms = { navController.navigate(Routes.studentTerms(it)) },
                refreshKey = refresh,
                profile = {
                    ProfileRoute(
                        onboarding,
                        extraContent = {
                            DailyGoalSetting()
                            LinkParentButton { navController.navigate(Routes.STUDENT_LINK_PARENT) }
                            DebugProfileExtras(navController)
                        },
                    )
                },
            )
        }
        composable(
            Routes.STUDENT_JOIN,
            arguments = listOf(navArgument(JOIN_KEY_ARG) { type = NavType.StringType; nullable = true; defaultValue = null }),
        ) { entry ->
            val scanned by entry.savedStateHandle.getStateFlow<String?>(SCANNED_KEY, null).collectAsStateWithLifecycle()
            JoinClassRoute(
                onScan = { navController.navigate(Routes.STUDENT_SCAN) },
                onJoined = {
                    navController.previousBackStackEntry?.savedStateHandle?.let { it[REFRESH] = (it.get<Int>(REFRESH) ?: 0) + 1 }
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                scannedKey = scanned,
            )
        }
        composable(Routes.STUDENT_SCAN) {
            ScanQrRoute(
                onKey = { key ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(SCANNED_KEY, key)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.TEACHER) { entry ->
            val refresh by entry.savedStateHandle.getStateFlow(REFRESH, 0).collectAsStateWithLifecycle()
            TeacherHomeRoute(
                onCreate = { navController.navigate(Routes.TEACHER_CREATE) },
                onOpen = { navController.navigate(Routes.teacherClassroom(it)) },
                onProfile = { navController.navigate(Routes.TEACHER_PROFILE) },
                onMakeCourse = { navController.navigate(Routes.notesUpload(personal = false)) },
                onOpenCourse = { navController.navigate(Routes.teacherCourse(it)) },
                refreshKey = refresh,
            )
        }
        composable(Routes.TEACHER_PROFILE) {
            ProfileRoute(onboarding, extraContent = { DebugProfileExtras(navController) })
        }
        composable(Routes.TEACHER_CREATE) {
            CreateClassroomRoute(
                onCreated = { id ->
                    navController.navigate(Routes.teacherKey(id)) { popUpTo(Routes.TEACHER) }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.TEACHER_KEY, arguments = listOf(navArgument(CLASSROOM_ID_ARG) { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString(CLASSROOM_ID_ARG).orEmpty()
            ClassroomKeyRoute(
                onDone = { navController.backToTeacherHome() },
                onDetails = { navController.navigate(Routes.teacherClassroom(id)) },
            )
        }
        composable(Routes.TEACHER_CLASSROOM, arguments = listOf(navArgument(CLASSROOM_ID_ARG) { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString(CLASSROOM_ID_ARG).orEmpty()
            ClassroomDetailRoute(
                onShowKey = { navController.navigate(Routes.teacherKey(id)) },
                onBack = { navController.popBackStack() },
                onDashboard = { navController.navigate(Routes.teacherDashboard(id)) },
            )
        }

        composable(Routes.PARENT) {
            ParentShell(
                profile = { ProfileRoute(onboarding, extraContent = { DebugProfileExtras(navController) }) },
                onAddChild = { navController.navigate(Routes.PARENT_ADD) },
                onConsent = { navController.navigate(Routes.parentConsent(it)) },
                onChildSettings = { navController.navigate(Routes.parentChild(it)) },
            )
        }

        notesDestinations(navController)
        learningDestinations(navController)
        familyDestinations(navController)
        teacherCourseDestinations(navController)
        debugDestinations(navController)
    }

    DebugOverlay(navController, Modifier.align(Alignment.TopEnd))

    notice?.let { n ->
        ReadyBanner(
            onOpen = {
                watcher.dismiss(n.uploadId)
                navController.navigate(Routes.notesGenerating(n.uploadId, teacher = navController.isTeacher()))
            },
            onDismiss = { watcher.dismiss(n.uploadId) },
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
    }
}

private fun NavHostController.isTeacher() = runCatching { getBackStackEntry(Routes.TEACHER) }.isSuccess

/** Notes → course: upload (shared ViewModel with camera and crop), then generation progress. */
private fun androidx.navigation.NavGraphBuilder.notesDestinations(navController: NavHostController) {
    composable(
        Routes.NOTES_UPLOAD,
        arguments = listOf(navArgument(PERSONAL_ARG) { type = NavType.BoolType; defaultValue = false }),
    ) { entry ->
        val personal = entry.arguments?.getBoolean(PERSONAL_ARG) ?: false
        NotesUploadRoute(
            viewModel = hiltViewModel(entry),
            onOpenCamera = { navController.navigate(Routes.NOTES_CAPTURE) },
            onEditPage = { navController.navigate(Routes.notesCrop(it)) },
            onStarted = { uploadId ->
                navController.navigate(Routes.notesGenerating(uploadId, teacher = !personal)) {
                    popUpTo(Routes.NOTES_UPLOAD) { inclusive = true }
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.NOTES_CAPTURE) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry(Routes.NOTES_UPLOAD) }
        val vm: NotesUploadViewModel = hiltViewModel(parent)
        val uploadState by vm.state.collectAsStateWithLifecycle()
        CaptureRoute(
            maxShots = MAX_PAGES - uploadState.pages.size,
            onDone = { uris ->
                vm.addPhotos(uris)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.NOTES_CROP, arguments = listOf(navArgument("pageId") { type = NavType.StringType })) { entry ->
        val parent = remember(entry) { navController.getBackStackEntry(Routes.NOTES_UPLOAD) }
        val vm: NotesUploadViewModel = hiltViewModel(parent)
        val state by vm.state.collectAsStateWithLifecycle()
        val pageId = entry.arguments?.getString("pageId").orEmpty()
        val page = state.pages.firstOrNull { it.id == pageId }
        if (page == null) {
            LaunchedEffect(Unit) { navController.popBackStack() }
        } else {
            CropRotateScreen(
                page = page,
                onRotate = { vm.rotate(pageId) },
                onDone = { crop ->
                    vm.setCrop(pageId, crop)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }
    }
    composable(
        Routes.NOTES_GENERATING,
        arguments = listOf(
            navArgument(UPLOAD_ID_ARG) { type = NavType.StringType },
            navArgument("teacher") { type = NavType.BoolType; defaultValue = true },
        ),
    ) { entry ->
        val teacher = entry.arguments?.getBoolean("teacher") ?: true
        GeneratingRoute(
            isTeacher = teacher,
            onOpenCourse = { courseId ->
                if (teacher) {
                    navController.navigate(Routes.teacherCourse(courseId)) { popUpTo(Routes.TEACHER) }
                } else {
                    navController.popBackStack(Routes.STUDENT, inclusive = false)
                }
            },
            onLeave = { navController.popBackStack() },
        )
    }
}

/** Student learning (M3): lesson path, the lesson player, the glossary and doubt chat. */
private fun androidx.navigation.NavGraphBuilder.learningDestinations(navController: NavHostController) {
    val askDoubt: (String, String?, String?, DoubtStart) -> Unit = { courseId, questionId, termKey, start ->
        navController.navigate(Routes.studentDoubt(courseId, questionId, termKey, start))
    }
    composable(Routes.STUDENT_PATH, arguments = listOf(navArgument("courseId") { type = NavType.StringType })) {
        LessonPathRoute(
            onPlay = { navController.navigate(Routes.studentLesson(it)) },
            onTerms = { navController.navigate(Routes.studentTerms(it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.STUDENT_LESSON, arguments = listOf(navArgument(LESSON_ARG) { type = NavType.StringType })) {
        LessonPlayerRoute(
            onExit = { navController.popBackStack() },
            onAskDoubt = askDoubt,
            onLinkParent = { navController.navigate(Routes.STUDENT_LINK_PARENT) { popUpTo(Routes.STUDENT) } },
        )
    }
    composable(Routes.STUDENT_TERMS, arguments = listOf(navArgument("courseId") { type = NavType.StringType })) {
        GlossaryRoute(
            onBack = { navController.popBackStack() },
            onAskDoubt = { courseId, key -> askDoubt(courseId, null, key, DoubtStart.NONE) },
        )
    }
    composable(
        Routes.STUDENT_DOUBT,
        arguments = listOf(
            navArgument("courseId") { type = NavType.StringType },
            navArgument(DOUBT_QUESTION_ARG) { type = NavType.StringType; defaultValue = "" },
            navArgument(DOUBT_TERM_ARG) { type = NavType.StringType; defaultValue = "" },
            navArgument(DOUBT_START_ARG) { type = NavType.StringType; defaultValue = "" },
        ),
    ) {
        DoubtRoute(onBack = { navController.popBackStack() })
    }
}

/** M4: the teacher's class dashboard, linking a parent, consent and child settings. */
private fun androidx.navigation.NavGraphBuilder.familyDestinations(navController: NavHostController) {
    composable(Routes.TEACHER_DASHBOARD, arguments = listOf(navArgument(CLASSROOM_ID_ARG) { type = NavType.StringType })) { entry ->
        val id = entry.arguments?.getString(CLASSROOM_ID_ARG).orEmpty()
        ClassDashboardRoute(
            onStudent = { navController.navigate(Routes.teacherStudent(id, it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        Routes.TEACHER_STUDENT,
        arguments = listOf(
            navArgument(CLASSROOM_ID_ARG) { type = NavType.StringType },
            navArgument(STUDENT_ID_ARG) { type = NavType.StringType },
        ),
    ) {
        StudentDetailRoute(onBack = { navController.popBackStack() })
    }
    composable(Routes.STUDENT_LINK_PARENT) {
        LinkParentRoute(onBack = { navController.popBackStack() })
    }
    composable(Routes.PARENT_ADD) { entry ->
        val scanned by entry.savedStateHandle.getStateFlow<String?>(SCANNED_KEY, null).collectAsStateWithLifecycle()
        AddChildRoute(
            onScan = { navController.navigate(Routes.PARENT_SCAN) },
            onLinked = { child ->
                if (child.consent == ConsentStatus.APPROVED) {
                    navController.popBackStack()
                } else {
                    navController.navigate(Routes.parentConsent(child.id)) { popUpTo(Routes.PARENT) }
                }
            },
            onBack = { navController.popBackStack() },
            scannedCode = scanned,
        )
    }
    composable(Routes.PARENT_SCAN) {
        ScanQrRoute(
            onKey = { code ->
                navController.previousBackStackEntry?.savedStateHandle?.set(SCANNED_KEY, code)
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() },
            extract = LinkCodes::findIn,
        )
    }
    composable(Routes.PARENT_CONSENT, arguments = listOf(navArgument(CHILD_ID_ARG) { type = NavType.StringType })) {
        ConsentRoute(onDone = { navController.popBackStack() })
    }
    composable(Routes.PARENT_CHILD, arguments = listOf(navArgument(CHILD_ID_ARG) { type = NavType.StringType })) {
        ChildSettingsRoute(
            onDone = { navController.popBackStack() },
            onConsent = { navController.navigate(Routes.parentConsent(it)) },
        )
    }
}

/** Teacher review: course → lesson → edit question; assign to classrooms. */
private fun androidx.navigation.NavGraphBuilder.teacherCourseDestinations(navController: NavHostController) {
    composable(Routes.TEACHER_COURSE, arguments = listOf(navArgument(COURSE_ID_ARG) { type = NavType.StringType })) { entry ->
        val courseId = entry.arguments?.getString(COURSE_ID_ARG).orEmpty()
        ReviewCourseRoute(
            onOpenLesson = { navController.navigate(Routes.teacherLesson(it)) },
            onAssign = { navController.navigate(Routes.teacherAssign(courseId)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(Routes.TEACHER_ASSIGN, arguments = listOf(navArgument(COURSE_ID_ARG) { type = NavType.StringType })) {
        AssignRoute(onDone = { navController.popBackStack() })
    }
    composable(Routes.TEACHER_LESSON, arguments = listOf(navArgument(LESSON_ID_ARG) { type = NavType.StringType })) { entry ->
        val lessonId = entry.arguments?.getString(LESSON_ID_ARG).orEmpty()
        ReviewLessonRoute(
            onEdit = { navController.navigate(Routes.teacherQuestion(lessonId, it)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        Routes.TEACHER_QUESTION,
        arguments = listOf(
            navArgument(LESSON_ID_ARG) { type = NavType.StringType },
            navArgument(QUESTION_ID_ARG) { type = NavType.StringType },
        ),
    ) {
        EditQuestionRoute(onDone = { navController.popBackStack() })
    }
}

private fun NavHostController.backToTeacherHome() {
    getBackStackEntry(Routes.TEACHER).savedStateHandle.let { it[REFRESH] = (it.get<Int>(REFRESH) ?: 0) + 1 }
    popBackStack(Routes.TEACHER, inclusive = false)
}

/** Onboarding navigation: forward steps stack up; reaching a home or signing out clears history. */
private fun NavHostController.goTo(nav: OnboardingNav) {
    when (nav) {
        OnboardingNav.Welcome -> navigate(Routes.SPLASH) { popUpTo(0) { inclusive = true } }
        OnboardingNav.Phone -> navigate(Routes.PHONE) { launchSingleTop = true; popUpTo(Routes.SPLASH) }
        OnboardingNav.Otp -> navigate(Routes.OTP)
        OnboardingNav.Language -> navigate(Routes.LANGUAGE) { popUpTo(Routes.PHONE) { inclusive = true } }
        OnboardingNav.Role -> navigate(Routes.ROLE)
        OnboardingNav.Name -> navigate(Routes.NAME)
        is OnboardingNav.Home -> navigate(Routes.home(nav.role)) { popUpTo(0) { inclusive = true } }
    }
}
