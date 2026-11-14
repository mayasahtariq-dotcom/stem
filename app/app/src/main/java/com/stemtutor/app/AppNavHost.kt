package com.stemtutor.app

import androidx.compose.runtime.Composable
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
import com.stemtutor.core.data.session.PendingJoinKey
import com.stemtutor.feature.onboarding.OnboardingNav
import com.stemtutor.feature.onboarding.choose.LanguageRoute
import com.stemtutor.feature.onboarding.choose.RoleRoute
import com.stemtutor.feature.onboarding.name.NameRoute
import com.stemtutor.feature.onboarding.otp.OtpRoute
import com.stemtutor.feature.onboarding.phone.PhoneRoute
import com.stemtutor.feature.onboarding.profile.ProfileRoute
import com.stemtutor.feature.onboarding.splash.SplashRoute
import com.stemtutor.feature.parent.ParentShell
import com.stemtutor.feature.student.StudentShell
import com.stemtutor.feature.student.join.JOIN_KEY_ARG
import com.stemtutor.feature.student.join.JoinClassRoute
import com.stemtutor.feature.student.join.ScanQrRoute
import com.stemtutor.feature.teacher.TeacherHomeRoute
import com.stemtutor.feature.teacher.classroom.CLASSROOM_ID_ARG
import com.stemtutor.feature.teacher.classroom.ClassroomDetailRoute
import com.stemtutor.feature.teacher.classroom.ClassroomKeyRoute
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
    const val TEACHER = "teacher"
    const val TEACHER_PROFILE = "teacher/profile"
    const val TEACHER_CREATE = "teacher/create"
    const val TEACHER_KEY = "teacher/classroom/{$CLASSROOM_ID_ARG}/key"
    const val TEACHER_CLASSROOM = "teacher/classroom/{$CLASSROOM_ID_ARG}"
    const val PARENT = "parent"

    fun join(key: String? = null) = if (key == null) "student/join" else "student/join?$JOIN_KEY_ARG=$key"
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
fun AppNavHost(pendingJoinKey: PendingJoinKey, navController: NavHostController = rememberNavController()) {
    val onboarding: (OnboardingNav) -> Unit = { navController.goTo(it) }

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
                refreshKey = refresh,
                profile = { ProfileRoute(onboarding, extraContent = { DebugProfileExtras(navController) }) },
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
            )
        }

        composable(Routes.PARENT) {
            ParentShell(profile = { ProfileRoute(onboarding, extraContent = { DebugProfileExtras(navController) }) })
        }

        debugDestinations(navController)
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
