package com.stemtutor.feature.teacher.classroom

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.classroom.JoinKeys
import com.stemtutor.core.data.model.ClassroomDetail
import com.stemtutor.core.data.model.Member
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorScreen
import com.stemtutor.core.designsystem.component.LoadingScreen
import com.stemtutor.core.designsystem.component.QrCode
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.component.TeacherHeader
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.BilingualString
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.core.network.ApiConfig
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.teacher.R
import com.stemtutor.feature.teacher.nameRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.stemtutor.core.designsystem.R as DsR

const val CLASSROOM_ID_ARG = "classroomId"

sealed interface ClassroomUiState {
    data object Loading : ClassroomUiState
    data class Error(val error: AppError) : ClassroomUiState
    data class Ready(val detail: ClassroomDetail, val webLink: String) : ClassroomUiState
}

/** Loads one of the teacher's classrooms; used by both the key/QR screen and the details screen. */
@HiltViewModel
class ClassroomViewModel @Inject constructor(
    private val classrooms: ClassroomRepository,
    private val config: ApiConfig,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val id: String = checkNotNull(savedState[CLASSROOM_ID_ARG])
    private val _state = MutableStateFlow<ClassroomUiState>(ClassroomUiState.Loading)
    val state: StateFlow<ClassroomUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.value = ClassroomUiState.Loading
        viewModelScope.launch {
            classrooms.classroom(id).fold(
                onSuccess = { _state.value = ClassroomUiState.Ready(it, JoinKeys.webLink(config.joinLinkHost, it.classroom.joinKey)) },
                onFailure = { _state.value = ClassroomUiState.Error(it.appError) },
            )
        }
    }
}

// ---- Key and QR -------------------------------------------------------------------------

@Composable
fun ClassroomKeyRoute(onDone: () -> Unit, onDetails: () -> Unit, viewModel: ClassroomViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val chooserTitle = bilingualStringResource(R.string.te_share_chooser)
    val shareText = (state as? ClassroomUiState.Ready)?.let { ready ->
        val c = ready.detail.classroom
        val msg = bilingualStringResource(R.string.te_share_message, c.name, c.joinKey)
        "${msg.regional}\n${msg.english}\n\n${ready.webLink}"
    }
    ClassroomKeyScreen(
        state = state,
        copied = copied,
        onCopy = { key ->
            clipboard.setText(AnnotatedString(key))
            copied = true
        },
        onShare = {
            shareText?.let {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, it)
                }
                context.startActivity(Intent.createChooser(send, chooserTitle.regional))
            }
        },
        onDetails = onDetails,
        onDone = onDone,
        onRetry = viewModel::load,
    )
}

@Composable
fun ClassroomKeyScreen(
    state: ClassroomUiState,
    copied: Boolean,
    onCopy: (String) -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    animate: Boolean = true,
) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        TeacherHeader(bilingualStringResource(R.string.te_key_title))
        when (state) {
            ClassroomUiState.Loading -> LoadingScreen(animate = animate)
            is ClassroomUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            is ClassroomUiState.Ready -> {
                val c = state.detail.classroom
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(c.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink, textAlign = TextAlign.Center)
                    // The key, large, inside a cream box (PRD), so it can be read off the board.
                    Text(
                        c.joinKey,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                        color = StemColors.Ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StemColors.KeyCream)
                            .padding(vertical = 18.dp)
                            .testTag("class_key"),
                    )
                    QrCode(
                        content = state.webLink,
                        contentDescription = bilingualStringResource(R.string.te_qr_description, c.joinKey).regional,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(StemColors.White).padding(10.dp),
                    )
                    BilingualText(R.string.te_key_hint, size = BilingualTextSize.Small, textAlign = TextAlign.Center)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ChunkyButton(
                            if (copied) R.string.te_copied else R.string.te_copy,
                            { onCopy(c.joinKey) },
                            Modifier.weight(1f),
                            ChunkyButtonStyle.White,
                        )
                        ChunkyButton(R.string.te_share, onShare, Modifier.weight(1f))
                    }
                    ChunkyButton(
                        text(R.string.te_members, state.detail.members.size),
                        onDetails,
                        Modifier.fillMaxWidth(),
                        ChunkyButtonStyle.White,
                    )
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    ChunkyButton(R.string.te_done, onDone, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        }
    }
}

@Composable
private fun text(res: Int, n: Int): BilingualString = bilingualStringResource(res, n)

// ---- Details and members ----------------------------------------------------------------

@Composable
fun ClassroomDetailRoute(onShowKey: () -> Unit, onBack: () -> Unit, viewModel: ClassroomViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ClassroomDetailScreen(state, onShowKey, onBack, viewModel::load)
}

@Composable
fun ClassroomDetailScreen(
    state: ClassroomUiState,
    onShowKey: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    animate: Boolean = true,
) {
    Column(Modifier.fillMaxSize().background(StemColors.TeacherBackground)) {
        val title = (state as? ClassroomUiState.Ready)?.detail?.classroom?.name.orEmpty()
        TeacherHeader(BilingualString(title, title.ifEmpty { " " }))
        when (state) {
            ClassroomUiState.Loading -> LoadingScreen(animate = animate)
            is ClassroomUiState.Error -> ErrorScreen(onRetry, messageRes = state.error.messageRes(), animate = animate)
            is ClassroomUiState.Ready -> {
                val c = state.detail.classroom
                val subject = bilingualStringResource(c.subject.nameRes())
                val grade = bilingualStringResource(DsR.string.grade_n, c.grade)
                val langs = c.allowedModes.map { bilingualStringResource(it.nameRes()) }
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BilingualText(
                        BilingualString(
                            "${subject.regional} · ${grade.regional} · ${langs.joinToString(", ") { it.regional }}",
                            "${subject.english} · ${grade.english} · ${langs.joinToString(", ") { it.english }} · ${c.joinKey}",
                        ),
                    )
                    ChunkyButton(R.string.te_show_key, onShowKey, Modifier.fillMaxWidth(), ChunkyButtonStyle.White)
                    BilingualText(text(R.string.te_members, state.detail.members.size), size = BilingualTextSize.Large)
                    if (state.detail.members.isEmpty()) {
                        SpeechBubble(R.string.te_no_members, Modifier.fillMaxWidth())
                    }
                    state.detail.members.forEach { MemberRow(it) }
                }
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    ChunkyButton(DsR.string.ds_back, onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
                }
            }
        }
    }
}

@Composable
private fun MemberRow(m: Member) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StemColors.White)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            m.name.take(1).uppercase(),
            fontWeight = FontWeight.ExtraBold,
            color = StemColors.Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(34.dp).clip(CircleShape).background(StemColors.KeyCream).padding(top = 6.dp),
        )
        Text(m.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StemColors.Ink)
    }
}

private val sample = ClassroomUiState.Ready(
    ClassroomDetail(
        TeacherClassroom("1", "Class 9-A Science", Subject.BIOLOGY, 9, "BI09-K4X2", LanguageMode.entries, 2),
        listOf(Member("a", "Ananya"), Member("b", "Ravi")),
    ),
    "https://mooshala.example/join/BI09-K4X2",
)

@BilingualPreviews
@Composable
private fun ClassroomKeyPreview() {
    StemTutorTheme { ClassroomKeyScreen(sample, false, {}, {}, {}, {}, {}, animate = false) }
}

@BilingualPreviews
@Composable
private fun ClassroomDetailPreview() {
    StemTutorTheme { ClassroomDetailScreen(sample, {}, {}, {}, animate = false) }
}
