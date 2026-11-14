package com.stemtutor.feature.parent

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.designsystem.component.BottomTabs
import com.stemtutor.core.designsystem.component.PromptScreen
import com.stemtutor.core.designsystem.component.Tab
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

data class ParentHomeUiState(val name: String = "")

@HiltViewModel
class ParentHomeViewModel @Inject constructor(profiles: ProfileRepository) : ViewModel() {
    private val _state = MutableStateFlow(ParentHomeUiState())
    val state: StateFlow<ParentHomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { _state.value = ParentHomeUiState(profiles.me().getOrNull()?.name.orEmpty()) }
    }
}

/** Placeholder until M4: the cow says linking a child is coming soon. Profile is the second tab. */
@Composable
fun ParentShell(profile: @Composable () -> Unit, viewModel: ParentHomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab(Sticker.Home, bilingualStringResource(DsR.string.ds_home)),
        Tab(Sticker.Duck, bilingualStringResource(DsR.string.ds_profile)),
    )
    Scaffold(bottomBar = { BottomTabs(tabs, tab, { tab = it }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (tab == 0) ParentHomeScreen(state) else profile()
        }
    }
}

@Composable
fun ParentHomeScreen(state: ParentHomeUiState, animate: Boolean = true) {
    val hello = bilingualStringResource(R.string.pa_hello, state.name)
    val soon = bilingualStringResource(R.string.pa_coming_soon)
    PromptScreen(
        bubble = BilingualString("${hello.regional}\n${soon.regional}", "${hello.english} ${soon.english}"),
        pose = CowPose.Heart,
        animate = animate,
        modifier = Modifier.statusBarsPadding(),
    )
}

@BilingualPreviews
@Composable
private fun ParentHomePreview() {
    StemTutorTheme { ParentHomeScreen(ParentHomeUiState("Meena"), animate = false) }
}
