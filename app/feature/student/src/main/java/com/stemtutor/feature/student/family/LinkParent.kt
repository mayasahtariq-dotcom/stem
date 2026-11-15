package com.stemtutor.feature.student.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.family.FamilyRepository
import com.stemtutor.core.data.model.LinkCode
import com.stemtutor.core.data.model.LinkedParent
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.QrCode
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.student.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

data class LinkParentUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val code: LinkCode? = null,
    val parents: List<LinkedParent> = emptyList(),
    val actionError: AppError? = null,
)

/** Profile → Link a parent (PRD M4): a code and QR that last 24 hours, and the linked parents. */
@HiltViewModel
class LinkParentViewModel @Inject constructor(private val family: FamilyRepository) : ViewModel() {
    private val _state = MutableStateFlow(LinkParentUiState())
    val state: StateFlow<LinkParentUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val parents = family.myParents()
            family.createLinkCode().fold(
                onSuccess = { code -> _state.update { it.copy(loading = false, code = code, parents = parents.getOrDefault(emptyList())) } },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
            )
        }
    }

    fun remove(parentId: String) {
        viewModelScope.launch {
            family.removeParent(parentId).fold(
                onSuccess = { _state.update { s -> s.copy(parents = s.parents.filterNot { it.id == parentId }) } },
                onFailure = { e -> _state.update { it.copy(actionError = e.appError) } },
            )
        }
    }
}

@Composable
fun LinkParentRoute(onBack: () -> Unit, viewModel: LinkParentViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LinkParentScreen(state, viewModel::load, viewModel::remove, onBack)
}

@Composable
fun LinkParentScreen(state: LinkParentUiState, onRetry: () -> Unit, onRemove: (String) -> Unit, onBack: () -> Unit, animate: Boolean = true) {
    Column(Modifier.fillMaxSize().background(StemColors.White)) {
        when {
            state.loading -> LoadingScreen(animate = animate)
            state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            else -> Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CowMascot(CowPose.Heart, width = 96.dp, animate = animate)
                SpeechBubble(R.string.fam_link_explain, Modifier.fillMaxWidth())
                state.code?.let { code ->
                    val desc = bilingualStringResource(R.string.fam_qr_desc)
                    QrCode(code.code, "${desc.regional} · ${desc.english}")
                    Text(code.code, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = StemColors.Ink, letterSpacing = 4.sp)
                    BilingualText(R.string.fam_code_24h, size = BilingualTextSize.Small)
                }
                BilingualText(R.string.fam_linked_parents, size = BilingualTextSize.Large, modifier = Modifier.fillMaxWidth())
                if (state.parents.isEmpty()) BilingualText(R.string.fam_no_parents, size = BilingualTextSize.Small, modifier = Modifier.fillMaxWidth())
                state.parents.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(StemColors.Soft).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
                            BilingualText(bilingualStringResource(R.string.fam_phone_ending, p.phoneLast4), size = BilingualTextSize.Small)
                        }
                        ChunkyButton(R.string.fam_remove, { onRemove(p.id) }, style = ChunkyButtonStyle.White)
                    }
                }
                state.actionError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            }
        }
        ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth().padding(16.dp), ChunkyButtonStyle.Ghost)
    }
}

/** Profile entry point for students. */
@Composable
fun LinkParentButton(onClick: () -> Unit) {
    ChunkyButton(R.string.fam_link_parent, onClick, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
}

@BilingualPreviews
@Composable
private fun LinkParentPreview() {
    StemTutorTheme {
        LinkParentScreen(
            LinkParentUiState(loading = false, code = LinkCode("KAV234", ""), parents = listOf(LinkedParent("p1", "Meena", "4821", true))),
            {}, {}, {}, animate = false,
        )
    }
}
