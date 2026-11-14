package com.stemtutor.core.data.classroom

import com.stemtutor.core.common.language.LanguageMode
import com.stemtutor.core.data.model.ClassPreview
import com.stemtutor.core.data.model.ClassroomDetail
import com.stemtutor.core.data.model.Member
import com.stemtutor.core.data.model.NewClassroom
import com.stemtutor.core.data.model.StudentClassroom
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.TeacherClassroom
import com.stemtutor.core.data.model.fromApi
import com.stemtutor.core.network.ClassroomCreateDto
import com.stemtutor.core.network.ClassroomDetailDto
import com.stemtutor.core.network.ClassroomDto
import com.stemtutor.core.network.JoinRequestDto
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.StudentClassroomDto
import com.stemtutor.core.network.apiCall

interface ClassroomRepository {
    suspend fun create(classroom: NewClassroom): Result<TeacherClassroom>

    suspend fun teacherClassrooms(): Result<List<TeacherClassroom>>

    suspend fun classroom(id: String): Result<ClassroomDetail>

    suspend fun preview(key: String): Result<ClassPreview>

    suspend fun join(key: String): Result<StudentClassroom>

    suspend fun myClassrooms(): Result<List<StudentClassroom>>
}

private fun List<String>.modes() = map { LanguageMode.fromApi(it) }.distinct()

internal fun ClassroomDto.toModel() = TeacherClassroom(
    id, name, Subject.fromCode(subject), grade, joinKey, allowedLanguageModes.modes(), studentCount,
)

internal fun ClassroomDetailDto.toModel() = ClassroomDetail(
    TeacherClassroom(id, name, Subject.fromCode(subject), grade, joinKey, allowedLanguageModes.modes(), studentCount),
    members.map { Member(it.id, it.name) },
)

internal fun StudentClassroomDto.toModel() =
    StudentClassroom(id, name, teacherName, Subject.fromCode(subject), grade, studentCount)

class RemoteClassroomRepository(private val api: StemApi) : ClassroomRepository {
    override suspend fun create(classroom: NewClassroom) = apiCall {
        api.createClassroom(
            ClassroomCreateDto(
                name = classroom.name.trim(),
                subject = classroom.subject.code,
                grade = classroom.grade,
                allowedLanguageModes = classroom.allowedModes.map { it.storageKey },
            ),
        ).toModel()
    }

    override suspend fun teacherClassrooms() = apiCall { api.teacherClassrooms().map { it.toModel() } }

    override suspend fun classroom(id: String) = apiCall { api.classroom(id).toModel() }

    override suspend fun preview(key: String) = apiCall {
        api.preview(key).let {
            ClassPreview(
                it.id, it.name, it.teacherName, Subject.fromCode(it.subject), it.grade, it.studentCount,
                it.allowedLanguageModes.modes(),
            )
        }
    }

    override suspend fun join(key: String) = apiCall { api.join(JoinRequestDto(key)).toModel() }

    override suspend fun myClassrooms() = apiCall { api.myClassrooms().map { it.toModel() } }
}
