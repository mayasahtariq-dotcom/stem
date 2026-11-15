package com.stemtutor.feature.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.family.FamilyRepository
import com.stemtutor.core.data.model.Child
import com.stemtutor.core.data.model.ChildPreview
import com.stemtutor.core.data.model.ConsentStatus
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.component.StemTextField
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val CHILD_ID_ARG = "childId"

// ---- add child -----------------------------------------------------------------------------

data class AddChildUiState(
    val code: String = "",
    val checking: Boolean = false,
    val preview: ChildPreview? = null,
    val linking: Boolean = false,
    val linked: Child? = null,
    val error: AppError? = null,
)

/** Add child (PRD M4): enter or scan the child's code, see who it is, then link. */
@HiltViewModel
class AddChildViewModel @Inject constructor(private val family: FamilyRepository) : ViewModel() {
    private val _state = MutableStateFlow(AddChildUiState())
    val state: StateFlow<AddChildUiState> = _state.asStateFlow()

    fun type(code: String) = _state.update { it.copy(code = code.uppercase().filter { c -> c.isLetterOrDigit() }.take(6), error = null, preview = null) }

    fun check(code: String = _state.value.code) {
        _state.update { it.copy(code = code, checking = true, error = null) }
        viewModelScope.launch {
            family.previewLink(code).fold(
                onSuccess = { p -> _state.update { it.copy(checking = false, preview = p) } },
                onFailure = { e -> _state.update { it.copy(checking = false, error = e.appError) } },
            )
        }
    }

    fun link() {
        _state.update { it.copy(linking = true, error = null) }
        viewModelScope.launch {
            family.link(_state.value.code).fold(
                onSuccess = { c -> _state.update { it.copy(linking = false, linked = c) } },
                onFailure = { e -> _state.update { it.copy(linking = false, error = e.appError) } },
            )
        }
    }
}

@Composable
fun AddChildRoute(
    onScan: () -> Unit,
    onLinked: (Child) -> Unit,
    onBack: () -> Unit,
    scannedCode: String? = null,
    viewModel: AddChildViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(scannedCode) { scannedCode?.let { viewModel.check(it.trim().uppercase()) } }
    LaunchedEffect(state.linked) { state.linked?.let(onLinked) }
    AddChildScreen(state, viewModel::type, { viewModel.check() }, viewModel::link, onScan, onBack)
}

@Composable
fun AddChildScreen(
    state: AddChildUiState,
    onType: (String) -> Unit,
    onCheck: () -> Unit,
    onLink: () -> Unit,
    onScan: () -> Unit,
    onBack: () -> Unit,
    animate: Boolean = true,
) {
    Column(
        Modifier.fillMaxSize().background(StemColors.White).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CowMascot(CowPose.Question, width = 96.dp, animate = animate)
        SpeechBubble(R.string.pa_add_child_help, Modifier.fillMaxWidth())
        StemTextField(
            value = state.code,
            onValueChange = onType,
            placeholder = bilingualStringResource(R.string.pa_code_hint),
            capitalization = KeyboardCapitalization.Characters,
            maxLength = 6,
            onDone = onCheck,
        )
        val preview = state.preview
        if (preview == null) {
            ChunkyButton(R.string.pa_check_code, onCheck, Modifier.fillMaxWidth(), enabled = state.code.length == 6 && !state.checking)
            ChunkyButton(R.string.pa_scan, onScan, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
        } else {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.Cream).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(preview.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = StemColors.Ink)
                preview.classes.forEach { Text("${it.name} · ${it.teacherName}", fontSize = 15.sp, color = StemColors.Ink) }
            }
            ChunkyButton(R.string.pa_link, onLink, Modifier.fillMaxWidth(), enabled = !state.linking)
        }
        state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
    }
}

// ---- consent -------------------------------------------------------------------------------

data class ChildUiState(
    val loading: Boolean = true,
    val error: AppError? = null,
    val child: Child? = null,
    val busy: Boolean = false,
    val actionError: AppError? = null,
    val confirm: ChildAction? = null,
    val done: Boolean = false,
)

enum class ChildAction { REVOKE, DELETE, UNLINK }

/** One linked child: consent approval and the child settings (revoke, delete, remove link). */
@HiltViewModel
class ChildViewModel @Inject constructor(
    private val family: FamilyRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val childId: String = checkNotNull(savedState[CHILD_ID_ARG])
    private val _state = MutableStateFlow(ChildUiState())
    val state: StateFlow<ChildUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            family.children().fold(
                onSuccess = { kids ->
                    val c = kids.firstOrNull { it.id == childId }
                    _state.update { it.copy(loading = false, child = c, done = c == null) }
                },
                onFailure = { e -> _state.update { it.copy(loading = false, error = e.appError) } },
            )
        }
    }

    fun approve() = act { family.approveConsent(childId).map { c -> _state.update { it.copy(child = c, done = true) } } }

    fun ask(action: ChildAction?) = _state.update { it.copy(confirm = action) }

    fun confirm() {
        val action = _state.value.confirm ?: return
        _state.update { it.copy(confirm = null) }
        when (action) {
            ChildAction.REVOKE -> act { family.revokeConsent(childId).map { c -> _state.update { it.copy(child = c) } } }
            ChildAction.DELETE -> act { family.requestDeletion(childId).map { _state.update { it.copy(done = true) } } }
            ChildAction.UNLINK -> act { family.unlink(childId).map { _state.update { it.copy(done = true) } } }
        }
    }

    private fun act(block: suspend () -> Result<Unit>) {
        _state.update { it.copy(busy = true, actionError = null) }
        viewModelScope.launch {
            block().onFailure { e -> _state.update { it.copy(actionError = e.appError) } }
            _state.update { it.copy(busy = false) }
        }
    }
}

@Composable
fun ConsentRoute(onDone: () -> Unit, viewModel: ChildViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    ConsentScreen(state, viewModel::approve, viewModel::load, onDone)
}

/** DPDP consent (PRD M4): what is collected, what isn't, then "I approve". */
@Composable
fun ConsentScreen(state: ChildUiState, onApprove: () -> Unit, onRetry: () -> Unit, onLater: () -> Unit, animate: Boolean = true) {
    when {
        state.loading -> LoadingScreen(animate = animate)
        state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        else -> Column(
            Modifier.fillMaxSize().background(StemColors.White).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val name = state.child?.name.orEmpty()
            BilingualText(bilingualStringResource(R.string.pa_consent_title, name), size = BilingualTextSize.Large)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.GreenTint).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BilingualText(R.string.pa_we_collect, regionalColor = StemColors.CorrectText)
                BilingualText(R.string.pa_we_collect_list, size = BilingualTextSize.Small)
            }
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(StemColors.Soft).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BilingualText(R.string.pa_we_dont, regionalColor = StemColors.Ink)
                BilingualText(R.string.pa_we_dont_list, size = BilingualTextSize.Small)
            }
            BilingualText(R.string.pa_consent_rights, size = BilingualTextSize.Small)
            state.actionError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            ChunkyButton(R.string.pa_i_approve, onApprove, Modifier.fillMaxWidth(), enabled = !state.busy)
            ChunkyButton(R.string.pa_later, onLater, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
}

// ---- child settings ------------------------------------------------------------------------

@Composable
fun ChildSettingsRoute(onDone: () -> Unit, onConsent: (String) -> Unit, viewModel: ChildViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    ChildSettingsScreen(state, viewModel::ask, viewModel::confirm, viewModel::load, { state.child?.let { onConsent(it.id) } }, onDone)
}

@Composable
fun ChildSettingsScreen(
    state: ChildUiState,
    onAsk: (ChildAction?) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onConsent: () -> Unit,
    onBack: () -> Unit,
    animate: Boolean = true,
) {
    when {
        state.loading -> LoadingScreen(animate = animate)
        state.error != null -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
        else -> Column(
            Modifier.fillMaxSize().background(StemColors.White).statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val child = state.child
            Text(child?.name.orEmpty(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = StemColors.Ink)
            if (child?.consent == ConsentStatus.APPROVED) {
                BilingualText(R.string.pa_consent_on, regionalColor = StemColors.CorrectText)
                ChunkyButton(R.string.pa_revoke, { onAsk(ChildAction.REVOKE) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.White, enabled = !state.busy)
            } else {
                BilingualText(R.string.pa_consent_off, regionalColor = StemColors.WrongText)
                ChunkyButton(R.string.pa_review_consent, onConsent, Modifier.fillMaxWidth())
            }
            ChunkyButton(R.string.pa_unlink, { onAsk(ChildAction.UNLINK) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.White, enabled = !state.busy)
            ChunkyButton(R.string.pa_delete, { onAsk(ChildAction.DELETE) }, Modifier.fillMaxWidth(), ChunkyButtonStyle.White, enabled = !state.busy)
            BilingualText(R.string.pa_delete_help, size = BilingualTextSize.Small)
            state.actionError?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
            ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }
    state.confirm?.let { action ->
        val (title, body) = when (action) {
            ChildAction.REVOKE -> R.string.pa_revoke to R.string.pa_revoke_confirm
            ChildAction.DELETE -> R.string.pa_delete to R.string.pa_delete_confirm
            ChildAction.UNLINK -> R.string.pa_unlink to R.string.pa_unlink_confirm
        }
        AlertDialog(
            onDismissRequest = { onAsk(null) },
            title = { BilingualText(title, size = BilingualTextSize.Large) },
            text = { BilingualText(body) },
            confirmButton = { TextButton(onClick = onConfirm) { BilingualText(R.string.pa_yes, regionalColor = StemColors.Red) } },
            dismissButton = { TextButton(onClick = { onAsk(null) }) { BilingualText(R.string.pa_no) } },
            containerColor = StemColors.White,
        )
    }
}

@BilingualPreviews
@Composable
private fun ConsentPreview() {
    StemTutorTheme {
        ConsentScreen(ChildUiState(loading = false, child = Child("k", "Ananya", ConsentStatus.PENDING, false, emptyList())), {}, {}, {}, animate = false)
    }
}

@BilingualPreviews
@Composable
private fun AddChildPreview() {
    StemTutorTheme { AddChildScreen(AddChildUiState(code = "KAV234"), {}, {}, {}, {}, {}, animate = false) }
}

/** Finds a parent link code (six letters/digits from the key alphabet) in scanned text. */
object LinkCodes {
    private val CODE = Regex("(?<![A-Z0-9])[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}(?![A-Z0-9])")

    fun findIn(raw: String): String? = CODE.find(raw.trim().uppercase())?.value
}
