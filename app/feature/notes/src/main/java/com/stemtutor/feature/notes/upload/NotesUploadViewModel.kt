package com.stemtutor.feature.notes.upload

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.CourseRepository
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.model.NewUpload
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.appError
import com.stemtutor.feature.notes.CropRect
import com.stemtutor.feature.notes.NotesImages
import com.stemtutor.feature.notes.PageDraft
import com.stemtutor.feature.notes.PageProcessor
import com.stemtutor.feature.notes.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val PERSONAL_ARG = "personal"
const val MAX_PAGES = 10

data class NotesUploadUiState(
    /** Students always make a "just me" course. */
    val personalOnly: Boolean = false,
    val pages: List<PageDraft> = emptyList(),
    val subject: Subject? = null,
    val grade: Int? = null,
    val forClass: Boolean = true,
    val classrooms: List<TeacherClassroom> = emptyList(),
    val classroomId: String? = null,
    /** A just-added photo that looks blurry: retake or keep? */
    val blurryPageId: String? = null,
    val reopenCamera: Boolean = false,
    val showTextEditor: Boolean = false,
    val textDraft: String = "",
    @StringRes val message: Int? = null,
    val submitting: Boolean = false,
    val error: AppError? = null,
    val startedUploadId: String? = null,
) {
    val personal get() = personalOnly || !forClass
    val canAddMore get() = pages.size < MAX_PAGES
    val canSubmit get() = pages.isNotEmpty() && !submitting && (personal || classroomId != null)
}

@HiltViewModel
class NotesUploadViewModel @Inject constructor(
    private val courses: CourseRepository,
    private val classrooms: ClassroomRepository,
    private val processor: PageProcessor,
    private val watcher: GenerationWatcher,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        NotesUploadUiState(personalOnly = savedState.get<Boolean>(PERSONAL_ARG) ?: false),
    )
    val state: StateFlow<NotesUploadUiState> = _state.asStateFlow()

    init {
        if (!_state.value.personalOnly) {
            viewModelScope.launch {
                classrooms.teacherClassrooms().onSuccess { list ->
                    _state.update {
                        it.copy(
                            classrooms = list,
                            classroomId = it.classroomId ?: list.firstOrNull()?.id,
                            forClass = list.isNotEmpty(),
                            subject = it.subject ?: list.firstOrNull()?.subject,
                            grade = it.grade ?: list.firstOrNull()?.grade,
                        )
                    }
                }
            }
        }
    }

    private fun newId() = UUID.randomUUID().toString()

    private fun room(n: Int): Int = (MAX_PAGES - _state.value.pages.size).coerceAtLeast(0).coerceAtMost(n)

    fun addPhotos(uris: List<String>) {
        val fits = room(uris.size)
        if (fits < uris.size) _state.update { it.copy(message = R.string.nt_too_many_pages) }
        val added = uris.take(fits).map { PageDraft(newId(), PageKind.PHOTO, uri = it) }
        _state.update { it.copy(pages = it.pages + added, reopenCamera = false) }
        added.forEach { page ->
            viewModelScope.launch {
                if (processor.isBlurry(requireNotNull(page.uri)) && _state.value.blurryPageId == null) {
                    _state.update { it.copy(blurryPageId = page.id) }
                }
            }
        }
    }

    fun addPdf(uri: String) {
        if (room(1) == 0) {
            _state.update { it.copy(message = R.string.nt_too_many_pages) }
            return
        }
        viewModelScope.launch {
            val size = processor.sizeOf(uri)
            if (size > NotesImages.MAX_PDF_BYTES) {
                _state.update { it.copy(message = R.string.nt_pdf_too_big) }
            } else {
                _state.update { it.copy(pages = it.pages + PageDraft(newId(), PageKind.PDF, uri = uri, sizeBytes = size)) }
            }
        }
    }

    fun openTextEditor() = _state.update { it.copy(showTextEditor = true, textDraft = "") }
    fun onTextDraft(text: String) = _state.update { it.copy(textDraft = text.take(20_000)) }
    fun closeTextEditor() = _state.update { it.copy(showTextEditor = false) }

    fun addText() {
        val text = _state.value.textDraft.trim()
        if (text.isEmpty() || room(1) == 0) return closeTextEditor()
        _state.update { it.copy(pages = it.pages + PageDraft(newId(), PageKind.TEXT, text = text), showTextEditor = false) }
    }

    /** Moves a page by [delta] places (the move buttons, and each step of a drag). */
    fun move(pageId: String, delta: Int) = _state.update { s ->
        val from = s.pages.indexOfFirst { it.id == pageId }
        if (from < 0) return@update s
        val to = (from + delta).coerceIn(0, s.pages.lastIndex)
        if (to == from) return@update s
        s.copy(pages = s.pages.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun delete(pageId: String) = _state.update { s ->
        s.copy(pages = s.pages.filterNot { it.id == pageId }, blurryPageId = s.blurryPageId.takeIf { it != pageId })
    }

    fun rotate(pageId: String) = updatePage(pageId) { it.copy(rotation = (it.rotation + 90) % 360, crop = CropRect.FULL) }

    fun setCrop(pageId: String, crop: CropRect) = updatePage(pageId) { it.copy(crop = crop) }

    private fun updatePage(pageId: String, change: (PageDraft) -> PageDraft) = _state.update { s ->
        s.copy(pages = s.pages.map { if (it.id == pageId) change(it) else it })
    }

    fun keepBlurry() = _state.update { it.copy(blurryPageId = null) }

    fun retakeBlurry() {
        val id = _state.value.blurryPageId ?: return
        delete(id)
        _state.update { it.copy(blurryPageId = null, reopenCamera = true) }
    }

    fun onCameraReopened() = _state.update { it.copy(reopenCamera = false) }

    fun setSubject(subject: Subject) = _state.update { it.copy(subject = subject) }
    fun setGrade(grade: Int) = _state.update { it.copy(grade = grade) }
    fun setForClass(forClass: Boolean) = _state.update { it.copy(forClass = forClass && it.classrooms.isNotEmpty()) }
    fun selectClassroom(id: String) = _state.update { s ->
        val c = s.classrooms.firstOrNull { it.id == id }
        s.copy(classroomId = id, subject = c?.subject ?: s.subject, grade = c?.grade ?: s.grade)
    }

    fun clearMessage() = _state.update { it.copy(message = null, error = null) }

    fun submit() {
        val s = _state.value
        if (!s.canSubmit) return
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            val result = runCatching { s.pages.map { processor.prepare(it) } }.mapCatching { pages ->
                courses.createAndStart(
                    NewUpload(
                        pages = pages,
                        subject = s.subject,
                        grade = s.grade,
                        classroomId = if (s.personal) null else s.classroomId,
                        personal = s.personal,
                    ),
                ).getOrThrow()
            }
            result.fold(
                onSuccess = { progress ->
                    watcher.watch(progress)
                    _state.update { it.copy(submitting = false, startedUploadId = progress.id) }
                },
                onFailure = { e -> _state.update { it.copy(submitting = false, error = e.appError) } },
            )
        }
    }

    fun onNavigated() = _state.update { it.copy(startedUploadId = null, pages = emptyList()) }
}
