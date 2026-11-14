package com.stemtutor.core.data.classroom

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.auth.FakeAuthRepository
import com.stemtutor.core.data.model.NewClassroom
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.profile.FakeProfileRepository
import com.stemtutor.core.network.ApiErrorCode
import com.stemtutor.core.network.appError
import com.stemtutor.core.network.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FakeClassroomRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var now = 0L

    private val store by lazy { PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "s.preferences_pb") } }
    private val auth by lazy { FakeAuthRepository(store) }
    private val profiles by lazy { FakeProfileRepository(auth, store) }
    private val repo by lazy { FakeClassroomRepository(auth, profiles) { now } }

    @After
    fun tearDown() = scope.cancel()

    private suspend fun signIn(phone: String, role: Role, mode: LanguageMode = LanguageMode.HINDI) {
        auth.verifyOtp(phone, FakeAuthRepository.FAKE_OTP).getOrThrow()
        profiles.save("User $phone", mode, listOf(role)).getOrThrow()
    }

    private fun Result<*>.code() = (exceptionOrNull()!!.appError as AppError.Api).code

    @Test
    fun studentPreviewsAndJoinsDemoClass() = runBlocking {
        signIn("9000000001", Role.STUDENT)
        val preview = repo.preview("bi09k4x2").getOrThrow()
        assertEquals("Mrs. Rao", preview.teacherName)
        assertEquals(38, preview.studentCount)
        assertEquals(39, repo.join("BI09-K4X2").getOrThrow().studentCount)
        assertEquals(1, repo.myClassrooms().getOrThrow().size)
        assertEquals(ApiErrorCode.ALREADY_MEMBER, repo.join("BI09-K4X2").code())
    }

    @Test
    fun demoKeysProduceEachError() = runBlocking {
        signIn("9000000002", Role.STUDENT, LanguageMode.HINDI)
        assertEquals(ApiErrorCode.KEY_NOT_FOUND, repo.preview("BI09-ZZZZ").code())
        assertEquals(ApiErrorCode.KEY_EXPIRED, repo.preview("BI09-XPRD").code())
        assertEquals(ApiErrorCode.LANGUAGE_NOT_ALLOWED, repo.preview("CH10-KNDA").code())
    }

    @Test
    fun eleventhAttemptInAnHourIsRefused() = runBlocking {
        signIn("9000000003", Role.STUDENT)
        repeat(10) { assertEquals(ApiErrorCode.KEY_NOT_FOUND, repo.preview("BI09-ZZZZ").code()) }
        assertEquals(ApiErrorCode.TOO_MANY_ATTEMPTS, repo.join("BI09-K4X2").code())
        now += 61 * 60 * 1000L
        assertTrue(repo.join("BI09-K4X2").isSuccess)
    }

    @Test
    fun teacherCreatesClassAndSeesIt() = runBlocking {
        signIn("9000000004", Role.TEACHER)
        val created = repo.create(NewClassroom("Class 8 Physics", Subject.PHYSICS, 8, LanguageMode.entries)).getOrThrow()
        assertTrue(created.joinKey, JoinKeys.isComplete(JoinKeys.sanitize(created.joinKey)))
        assertTrue(created.joinKey.startsWith("PH08-"))
        assertEquals(listOf(created.id), repo.teacherClassrooms().getOrThrow().map { it.id })
    }

    @Test
    fun studentCannotCreateAndTeacherCannotJoin() = runBlocking {
        signIn("9000000005", Role.STUDENT)
        assertEquals(
            ApiErrorCode.NOT_A_TEACHER,
            repo.create(NewClassroom("x", Subject.BIOLOGY, 9, LanguageMode.entries)).code(),
        )
        signIn("9000000006", Role.TEACHER)
        assertEquals(ApiErrorCode.NOT_A_STUDENT, repo.join("BI09-K4X2").code())
    }
}
