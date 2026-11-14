package com.stemtutor.feature.notes

import androidx.lifecycle.SavedStateHandle
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.auth.AuthSession
import com.stemtutor.core.data.classroom.ClassroomRepository
import com.stemtutor.core.data.course.FakeCourseRepository
import com.stemtutor.core.data.course.GenerationWatcher
import com.stemtutor.core.data.model.ClassPreview
import com.stemtutor.core.data.model.ClassroomDetail
import com.stemtutor.core.data.model.NewClassroom
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.PageUpload
import com.stemtutor.core.data.model.StudentClassroom
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.feature.notes.upload.MAX_PAGES
import com.stemtutor.feature.notes.upload.NotesUploadViewModel
import com.stemtutor.feature.notes.upload.PERSONAL_ARG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeProcessor(private val blurry: Set<String> = emptySet()) : PageProcessor {
    override suspend fun isBlurry(uri: String) = uri in blurry
    override suspend fun sizeOf(uri: String) = if (uri.contains("big")) 11L * 1024 * 1024 else 1000L
    override suspend fun prepare(page: PageDraft) = PageUpload(page.kind, bytes = ByteArray(10), text = page.text)
}

private object SignedIn : AuthRepository {
    override val session = flowOf(AuthSession("u1", "+919000000000"))
    override suspend fun currentSession() = AuthSession("u1", "+919000000000")
    override suspend fun sendOtp(phone: String) = Result.success(Unit)
    override suspend fun verifyOtp(phone: String, code: String) = Result.success(AuthSession("u1", phone))
    override suspend fun signOut() = Unit
    override suspend fun accessToken() = "t"
    override suspend fun refreshAccessToken() = "t"
}

private object NoClassrooms : ClassroomRepository {
    override suspend fun create(classroom: NewClassroom) = Result.failure<TeacherClassroom>(UnsupportedOperationException())
    override suspend fun teacherClassrooms() = Result.success(emptyList<TeacherClassroom>())
    override suspend fun classroom(id: String) = Result.failure<ClassroomDetail>(UnsupportedOperationException())
    override suspend fun preview(key: String) = Result.failure<ClassPreview>(UnsupportedOperationException())
    override suspend fun join(key: String) = Result.failure<StudentClassroom>(UnsupportedOperationException())
    override suspend fun myClassrooms() = Result.success(emptyList<StudentClassroom>())
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesUploadViewModelTest {
    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(processor: PageProcessor = FakeProcessor()): NotesUploadViewModel {
        val courses = FakeCourseRepository(SignedIn)
        return NotesUploadViewModel(
            courses, NoClassrooms, processor, GenerationWatcher(courses),
            SavedStateHandle(mapOf(PERSONAL_ARG to true)),
        )
    }

    private fun NotesUploadViewModel.ids() = state.value.pages.map { it.uri ?: it.text }

    @Test
    fun reorderingMovesPagesAndStopsAtTheEnds() {
        val vm = vm()
        vm.addPhotos(listOf("a", "b", "c"))
        val (a, _, c) = vm.state.value.pages.map { it.id }
        vm.move(a, 1)
        assertEquals(listOf("b", "a", "c"), vm.ids())
        vm.move(c, -2)
        assertEquals(listOf("c", "b", "a"), vm.ids())
        vm.move(c, -1) // already first
        assertEquals(listOf("c", "b", "a"), vm.ids())
    }

    @Test
    fun deletingRemovesOnlyThatPage() {
        val vm = vm()
        vm.addPhotos(listOf("a", "b"))
        vm.onTextDraft("typed notes")
        vm.addText()
        vm.delete(vm.state.value.pages[1].id)
        assertEquals(listOf("a", "typed notes"), vm.ids())
        assertEquals(listOf(PageKind.PHOTO, PageKind.TEXT), vm.state.value.pages.map { it.kind })
    }

    @Test
    fun atMostTenPages() {
        val vm = vm()
        vm.addPhotos((1..12).map { "p$it" })
        assertEquals(MAX_PAGES, vm.state.value.pages.size)
        assertEquals(R.string.nt_too_many_pages, vm.state.value.message)
    }

    @Test
    fun pdfOverTenMegabytesIsRefused() {
        val vm = vm()
        vm.addPdf("content://big.pdf")
        assertTrue(vm.state.value.pages.isEmpty())
        assertEquals(R.string.nt_pdf_too_big, vm.state.value.message)
        vm.addPdf("content://small.pdf")
        assertEquals(1, vm.state.value.pages.size)
    }

    @Test
    fun blurryPhotoAsksRetakeOrKeep() {
        val vm = vm(FakeProcessor(blurry = setOf("blurry")))
        vm.addPhotos(listOf("sharp", "blurry"))
        assertNotNull(vm.state.value.blurryPageId)
        vm.retakeBlurry()
        assertEquals(listOf("sharp"), vm.ids())
        assertTrue(vm.state.value.reopenCamera)

        vm.addPhotos(listOf("blurry"))
        vm.keepBlurry()
        assertNull(vm.state.value.blurryPageId)
        assertEquals(listOf("sharp", "blurry"), vm.ids())
    }

    @Test
    fun rotatingResetsTheCrop() {
        val vm = vm()
        vm.addPhotos(listOf("a"))
        val id = vm.state.value.pages[0].id
        vm.setCrop(id, CropRect(0.1f, 0.1f, 0.9f, 0.9f))
        vm.rotate(id)
        assertEquals(90, vm.state.value.pages[0].rotation)
        assertEquals(CropRect.FULL, vm.state.value.pages[0].crop)
    }

    @Test
    fun submitStartsTheUploadAndHandsOverItsId() {
        val vm = vm()
        vm.addPhotos(listOf("a"))
        vm.submit()
        assertNotNull(vm.state.value.startedUploadId)
    }
}
