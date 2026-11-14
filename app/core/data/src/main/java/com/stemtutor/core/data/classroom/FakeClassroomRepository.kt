package com.stemtutor.core.data.classroom

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.auth.AuthRepository
import com.stemtutor.core.data.model.ClassPreview
import com.stemtutor.core.data.model.ClassroomDetail
import com.stemtutor.core.data.model.Member
import com.stemtutor.core.data.model.NewClassroom
import com.stemtutor.core.data.model.Role
import com.stemtutor.core.data.model.StudentClassroom
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.data.profile.ProfileRepository
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * In-memory classrooms for debug builds and tests, following the backend's rules. Demo keys
 * (also listed in docs/demos/M1.md):
 * - BI09-K4X2: Mrs. Rao's Class 9-A Science (joinable)
 * - BI09-XPRD: an expired key
 * - CH10-KNDA: a Kannada-only class (refused for Hindi-mode students)
 * - any other well-formed key: not found
 * Joining twice gives ALREADY_MEMBER, and the 11th preview/join in an hour TOO_MANY_ATTEMPTS.
 */
class FakeClassroomRepository(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ClassroomRepository {
    private data class Room(
        val classroom: TeacherClassroom,
        val teacherId: String,
        val teacherName: String,
        val expired: Boolean = false,
        val members: MutableList<Member> = mutableListOf(),
        val seedCount: Int = 0,
    ) {
        val count get() = seedCount + members.size
    }

    private val lock = Mutex()
    private val rooms = mutableListOf(
        seed("BI09-K4X2", "Class 9-A Science", Subject.BIOLOGY, 9, LanguageMode.entries, 38),
        seed("BI09-XPRD", "Class 9-C Science", Subject.BIOLOGY, 9, LanguageMode.entries, 12, expired = true),
        seed("CH10-KNDA", "Class 10-B Chemistry", Subject.CHEMISTRY, 10, listOf(LanguageMode.KANNADA), 30),
    )
    private val attempts = mutableMapOf<String, MutableList<Long>>()

    private fun seed(
        key: String,
        name: String,
        subject: Subject,
        grade: Int,
        modes: List<LanguageMode>,
        count: Int,
        expired: Boolean = false,
    ) = Room(
        TeacherClassroom(UUID.nameUUIDFromBytes(key.toByteArray()).toString(), name, subject, grade, key, modes, count),
        teacherId = "fake-teacher-rao",
        teacherName = "Mrs. Rao",
        expired = expired,
        seedCount = count,
    )

    private fun fail(code: ApiErrorCode): Nothing = throw AppException(AppError.Api(code))

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = runCatching { lock.withLock { block() } }

    private suspend fun userId(): String = auth.currentSession()?.userId ?: fail(ApiErrorCode.UNAUTHORIZED)

    private suspend fun profileWith(role: Role) = profiles.me().getOrThrow().also {
        if (!it.registered) fail(ApiErrorCode.PROFILE_INCOMPLETE)
        if (role !in it.roles) fail(if (role == Role.TEACHER) ApiErrorCode.NOT_A_TEACHER else ApiErrorCode.NOT_A_STUDENT)
    }

    override suspend fun create(classroom: NewClassroom) = guarded {
        val me = profileWith(Role.TEACHER)
        var key: String
        do {
            key = JoinKeys.generate(classroom.subject, classroom.grade)
        } while (rooms.any { it.classroom.joinKey == key })
        val created = TeacherClassroom(
            UUID.randomUUID().toString(), classroom.name.trim(), classroom.subject, classroom.grade, key,
            classroom.allowedModes.distinct(), 0,
        )
        rooms += Room(created, me.id, me.name)
        created
    }

    override suspend fun teacherClassrooms() = guarded {
        val id = userId()
        rooms.filter { it.teacherId == id }.reversed().map { it.classroom.copy(studentCount = it.count) }
    }

    override suspend fun classroom(id: String) = guarded {
        val me = userId()
        val room = rooms.firstOrNull { it.classroom.id == id && it.teacherId == me } ?: fail(ApiErrorCode.NOT_FOUND)
        ClassroomDetail(room.classroom.copy(studentCount = room.count), room.members.sortedBy { it.name })
    }

    private suspend fun findJoinable(rawKey: String): Pair<Room, com.stemtutor.core.data.model.Profile> {
        val me = profileWith(Role.STUDENT)
        val now = clock()
        val recent = attempts.getOrPut(me.id) { mutableListOf() }.apply { removeAll { now - it > HOUR_MS } }
        if (recent.size >= ATTEMPTS_PER_HOUR) fail(ApiErrorCode.TOO_MANY_ATTEMPTS)
        recent += now
        val key = JoinKeys.format(JoinKeys.sanitize(rawKey))
        val room = rooms.firstOrNull { it.classroom.joinKey == key } ?: fail(ApiErrorCode.KEY_NOT_FOUND)
        if (room.expired) fail(ApiErrorCode.KEY_EXPIRED)
        if (room.members.any { it.id == me.id }) fail(ApiErrorCode.ALREADY_MEMBER)
        if (me.languageMode !in room.classroom.allowedModes) fail(ApiErrorCode.LANGUAGE_NOT_ALLOWED)
        return room to me
    }

    override suspend fun preview(key: String) = guarded {
        val (room, _) = findJoinable(key)
        with(room.classroom) {
            ClassPreview(id, name, room.teacherName, subject, grade, room.count, allowedModes)
        }
    }

    override suspend fun join(key: String) = guarded {
        val (room, me) = findJoinable(key)
        room.members += Member(me.id, me.name)
        room.toStudentClassroom()
    }

    override suspend fun myClassrooms() = guarded {
        val id = userId()
        rooms.filter { r -> r.members.any { it.id == id } }.map { it.toStudentClassroom() }
    }

    private fun Room.toStudentClassroom() = with(classroom) {
        StudentClassroom(id, name, teacherName, subject, grade, count)
    }

    private companion object {
        const val ATTEMPTS_PER_HOUR = 10
        const val HOUR_MS = 60 * 60 * 1000L
    }
}
