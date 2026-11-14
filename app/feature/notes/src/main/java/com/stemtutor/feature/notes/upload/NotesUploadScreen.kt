package com.stemtutor.feature.notes.upload

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.stemtutor.core.data.model.GRADES
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.designsystem.component.BilingualText
import com.stemtutor.core.designsystem.component.BilingualTextSize
import com.stemtutor.core.designsystem.component.ChoiceCard
import com.stemtutor.core.designsystem.component.ChunkyButton
import com.stemtutor.core.designsystem.component.ChunkyButtonStyle
import com.stemtutor.core.designsystem.component.ErrorLine
import com.stemtutor.core.designsystem.component.SpeechBubble
import com.stemtutor.core.designsystem.icon.Sticker
import com.stemtutor.core.designsystem.icon.StickerIcon
import com.stemtutor.core.designsystem.mascot.CowMascot
import com.stemtutor.core.designsystem.mascot.CowPose
import com.stemtutor.core.designsystem.preview.BilingualPreviews
import com.stemtutor.core.designsystem.text.bilingualStringResource
import com.stemtutor.core.designsystem.text.messageRes
import com.stemtutor.core.designsystem.theme.StemColors
import com.stemtutor.core.designsystem.theme.StemTutorTheme
import com.stemtutor.feature.notes.PageDraft
import com.stemtutor.feature.notes.R
import com.stemtutor.core.designsystem.R as DsR

@Composable
fun NotesUploadRoute(
    viewModel: NotesUploadViewModel,
    onOpenCamera: () -> Unit,
    onEditPage: (String) -> Unit,
    onStarted: (String) -> Unit,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PAGES)) { uris ->
        if (uris.isNotEmpty()) viewModel.addPhotos(uris.map { it.toString() })
    }
    val pickPdf = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.addPdf(it.toString()) }
    }
    val openCamera by rememberUpdatedState(onOpenCamera)
    LaunchedEffect(state.reopenCamera) {
        if (state.reopenCamera) {
            viewModel.onCameraReopened()
            openCamera()
        }
    }
    LaunchedEffect(state.startedUploadId) {
        state.startedUploadId?.let {
            viewModel.onNavigated()
            onStarted(it)
        }
    }
    NotesUploadScreen(
        state = state,
        actions = NotesUploadActions(
            onCamera = onOpenCamera,
            onGallery = { pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onPdf = { pickPdf.launch(arrayOf("application/pdf")) },
            onType = viewModel::openTextEditor,
            onTextDraft = viewModel::onTextDraft,
            onAddText = viewModel::addText,
            onCloseText = viewModel::closeTextEditor,
            onMove = viewModel::move,
            onDelete = viewModel::delete,
            onEdit = onEditPage,
            onKeepBlurry = viewModel::keepBlurry,
            onRetakeBlurry = viewModel::retakeBlurry,
            onSubject = viewModel::setSubject,
            onGrade = viewModel::setGrade,
            onForClass = viewModel::setForClass,
            onClassroom = viewModel::selectClassroom,
            onSubmit = viewModel::submit,
            onBack = onBack,
        ),
    )
}

class NotesUploadActions(
    val onCamera: () -> Unit = {},
    val onGallery: () -> Unit = {},
    val onPdf: () -> Unit = {},
    val onType: () -> Unit = {},
    val onTextDraft: (String) -> Unit = {},
    val onAddText: () -> Unit = {},
    val onCloseText: () -> Unit = {},
    val onMove: (String, Int) -> Unit = { _, _ -> },
    val onDelete: (String) -> Unit = {},
    val onEdit: (String) -> Unit = {},
    val onKeepBlurry: () -> Unit = {},
    val onRetakeBlurry: () -> Unit = {},
    val onSubject: (Subject) -> Unit = {},
    val onGrade: (Int) -> Unit = {},
    val onForClass: (Boolean) -> Unit = {},
    val onClassroom: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotesUploadScreen(state: NotesUploadUiState, actions: NotesUploadActions, animate: Boolean = true) {
    Column(Modifier.fillMaxSize().background(StemColors.White).statusBarsPadding().imePadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CowMascot(if (state.submitting) CowPose.Question else CowPose.Thumbs, width = 72.dp, animate = animate)
                SpeechBubble(bilingualStringResource(R.string.nt_add_pages_hint), Modifier.weight(1f))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceButton(Sticker.Camera, R.string.nt_camera, state.canAddMore, actions.onCamera)
                SourceButton(Sticker.Image, R.string.nt_gallery, state.canAddMore, actions.onGallery)
                SourceButton(Sticker.Doc, R.string.nt_pdf, state.canAddMore, actions.onPdf)
                SourceButton(Sticker.Pencil, R.string.nt_type, state.canAddMore, actions.onType)
            }
            BilingualText(bilingualStringResource(R.string.nt_pages_n, state.pages.size), size = BilingualTextSize.Small)
            if (state.pages.isNotEmpty()) PageStrip(state.pages, actions)

            BilingualText(R.string.nt_subject_grade, size = BilingualTextSize.Small)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Subject.entries.forEach { s ->
                    ChoiceCard(selected = state.subject == s, onClick = { actions.onSubject(s) }, modifier = Modifier.weight(1f)) {
                        BilingualText(s.nameRes(), size = BilingualTextSize.Small)
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GRADES.forEach { g ->
                    ChoiceCard(selected = state.grade == g, onClick = { actions.onGrade(g) }, modifier = Modifier.width(56.dp)) {
                        Text("$g", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StemColors.Ink)
                    }
                }
            }
            if (!state.personalOnly && state.classrooms.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceCard(state.forClass, { actions.onForClass(true) }, Modifier.weight(1f)) {
                        BilingualText(R.string.nt_for_class, size = BilingualTextSize.Small)
                    }
                    ChoiceCard(!state.forClass, { actions.onForClass(false) }, Modifier.weight(1f)) {
                        BilingualText(R.string.nt_just_me, size = BilingualTextSize.Small)
                    }
                }
                if (state.forClass) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.classrooms.forEach { c ->
                            ChoiceCard(state.classroomId == c.id, { actions.onClassroom(c.id) }) {
                                Text(c.name, fontWeight = FontWeight.Bold, color = StemColors.Ink)
                            }
                        }
                    }
                }
            } else {
                BilingualText(R.string.nt_just_me, size = BilingualTextSize.Small, regionalColor = StemColors.Brown, englishColor = StemColors.Brown)
            }
            state.message?.let { ErrorLine(bilingualStringResource(it)) }
            state.error?.let { ErrorLine(bilingualStringResource(it.messageRes())) }
        }
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.submitting) {
                BilingualText(R.string.nt_preparing, size = BilingualTextSize.Small, modifier = Modifier.fillMaxWidth())
            }
            ChunkyButton(R.string.nt_make, actions.onSubmit, Modifier.fillMaxWidth().testTag("make_course"), enabled = state.canSubmit)
            ChunkyButton(DsR.string.ds_back, actions.onBack, Modifier.fillMaxWidth(), ChunkyButtonStyle.Ghost)
        }
    }

    if (state.showTextEditor) TextPageDialog(state.textDraft, actions)
    if (state.blurryPageId != null) BlurDialog(actions)
}

@Composable
private fun SourceButton(icon: Sticker, label: Int, enabled: Boolean, onClick: () -> Unit) {
    ChoiceCard(selected = false, onClick = { if (enabled) onClick() }, modifier = Modifier.width(150.dp)) {
        StickerIcon(icon, size = 30.dp)
        BilingualText(label, size = BilingualTextSize.Small)
    }
}

private val THUMB = 96.dp

/** Thumbnails: long-press and drag sideways to reorder, or use the arrow buttons; tap to crop. */
@Composable
private fun PageStrip(pages: List<PageDraft>, actions: NotesUploadActions) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("page_strip")) {
        itemsIndexed(pages, key = { _, p -> p.id }) { index, page ->
            var dragX by remember(page.id) { mutableFloatStateOf(0f) }
            val stepPx = with(androidx.compose.ui.platform.LocalDensity.current) { (THUMB + 10.dp).toPx() }
            val label = bilingualStringResource(R.string.nt_page_n, index + 1)
            Column(
                Modifier
                    .offset { IntOffset(dragX.toInt(), 0) }
                    .pointerInput(page.id) {
                        detectDragGesturesAfterLongPress(
                            onDragEnd = { dragX = 0f },
                            onDragCancel = { dragX = 0f },
                        ) { change, amount ->
                            change.consume()
                            dragX += amount.x
                            if (dragX > stepPx / 2) {
                                actions.onMove(page.id, 1)
                                dragX -= stepPx
                            } else if (dragX < -stepPx / 2) {
                                actions.onMove(page.id, -1)
                                dragX += stepPx
                            }
                        }
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .size(THUMB)
                        .clip(RoundedCornerShape(14.dp))
                        .background(StemColors.Soft)
                        .clickable(enabled = page.kind == PageKind.PHOTO) { actions.onEdit(page.id) }
                        .semantics { contentDescription = "${label.regional}. ${label.english}" },
                    contentAlignment = Alignment.Center,
                ) {
                    when (page.kind) {
                        PageKind.PHOTO -> AsyncImage(
                            model = page.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().rotate(page.rotation.toFloat()),
                        )
                        PageKind.PDF -> StickerIcon(Sticker.Doc, size = 44.dp)
                        PageKind.TEXT -> StickerIcon(Sticker.Pencil, size = 44.dp)
                    }
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.ExtraBold,
                        color = StemColors.White,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StemColors.Ink.copy(alpha = 0.6f))
                            .padding(horizontal = 6.dp),
                    )
                }
                Row {
                    IconAction(R.string.nt_move_left, index > 0, { actions.onMove(page.id, -1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = StemColors.Ink)
                    }
                    IconAction(R.string.nt_delete_page, true, { actions.onDelete(page.id) }) {
                        StickerIcon(Sticker.Trash, size = 22.dp)
                    }
                    IconAction(R.string.nt_move_right, index < pages.lastIndex, { actions.onMove(page.id, 1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = StemColors.Ink)
                    }
                }
            }
        }
    }
}

/** A 48 dp tap target with a bilingual label for screen readers; dimmed when disabled. */
@Composable
private fun IconAction(label: Int, enabled: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val text = bilingualStringResource(label)
    Box(
        Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clickable(enabled = enabled, onClickLabel = text.regional, onClick = onClick)
            .semantics { contentDescription = "${text.regional}. ${text.english}" },
        contentAlignment = Alignment.Center,
    ) { icon() }
}

@Composable
private fun TextPageDialog(draft: String, actions: NotesUploadActions) {
    val placeholder = bilingualStringResource(R.string.nt_typed_placeholder)
    AlertDialog(
        onDismissRequest = actions.onCloseText,
        title = { BilingualText(R.string.nt_type) },
        text = {
            OutlinedTextField(
                value = draft,
                onValueChange = actions.onTextDraft,
                placeholder = { Text("${placeholder.regional} · ${placeholder.english}") },
                minLines = 6,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { ChunkyButton(R.string.nt_add_text, actions.onAddText, enabled = draft.isNotBlank()) },
        dismissButton = { ChunkyButton(DsR.string.ds_cancel, actions.onCloseText, style = ChunkyButtonStyle.Ghost) },
    )
}

@Composable
private fun BlurDialog(actions: NotesUploadActions) {
    AlertDialog(
        onDismissRequest = actions.onKeepBlurry,
        text = { BilingualText(R.string.nt_blurry) },
        confirmButton = { ChunkyButton(R.string.nt_retake, actions.onRetakeBlurry) },
        dismissButton = { ChunkyButton(R.string.nt_keep, actions.onKeepBlurry, style = ChunkyButtonStyle.White) },
    )
}

private fun Subject.nameRes() = when (this) {
    Subject.BIOLOGY -> DsR.string.subject_biology
    Subject.CHEMISTRY -> DsR.string.subject_chemistry
    Subject.PHYSICS -> DsR.string.subject_physics
}

@BilingualPreviews
@Composable
private fun NotesUploadPreview() {
    StemTutorTheme {
        NotesUploadScreen(
            NotesUploadUiState(
                pages = listOf(
                    PageDraft("1", PageKind.TEXT, text = "Notes"),
                    PageDraft("2", PageKind.PDF, uri = "x"),
                ),
                subject = Subject.BIOLOGY,
                grade = 9,
                personalOnly = true,
            ),
            NotesUploadActions(),
            animate = false,
        )
    }
}
