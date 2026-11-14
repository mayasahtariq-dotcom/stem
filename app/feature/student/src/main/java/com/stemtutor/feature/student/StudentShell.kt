package com.stemtutor.feature.student

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stemtutor.core.designsystem.component.BottomTabs
import com.stemtutor.core.designsystem.component.Tab
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.student.home.StudentClassTabScreen
import com.stemtutor.feature.student.home.StudentClassesViewModel
import com.stemtutor.feature.student.home.StudentHomeScreen
import com.stemtutor.core.designsystem.R as DsR

/**
 * The student's main screen with the bottom bar: Home, Learn (coming soon until M3), Class and
 * Profile. [profile] is supplied by the app (the shared profile screen).
 */
@Composable
fun StudentShell(
    onJoin: () -> Unit,
    profile: @Composable () -> Unit,
    refreshKey: Int = 0,
    viewModel: StudentClassesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(refreshKey) { if (refreshKey > 0) viewModel.load() }
    val tabs = listOf(
        Tab(Sticker.Home, bilingualStringResource(R.string.st_tab_home)),
        Tab(Sticker.Book, bilingualStringResource(R.string.st_tab_learn)),
        Tab(Sticker.School, bilingualStringResource(R.string.st_tab_class)),
        Tab(Sticker.Duck, bilingualStringResource(DsR.string.ds_profile)),
    )
    Scaffold(bottomBar = { BottomTabs(tabs, tab, { tab = it }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> StudentHomeScreen(state, onJoin, viewModel::load)
                1 -> LearnComingSoon()
                2 -> StudentClassTabScreen(state, onJoin, viewModel::load)
                else -> profile()
            }
        }
    }
}

@Composable
fun LearnComingSoon(animate: Boolean = true) {
    PromptScreen(bubble = bilingualStringResource(R.string.st_learn_soon), pose = CowPose.Sleep, animate = animate)
}

@BilingualPreviews
@Composable
private fun LearnComingSoonPreview() {
    StemTutorTheme { LearnComingSoon(animate = false) }
}
