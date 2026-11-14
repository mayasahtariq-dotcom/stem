package com.stemtutor.core.data.health

import com.stemtutor.core.network.ClassroomCreateDto
import com.stemtutor.core.network.ClassroomDetailDto
import com.stemtutor.core.network.ClassroomDto
import com.stemtutor.core.network.ClassroomPreviewDto
import com.stemtutor.core.network.HealthDto
import com.stemtutor.core.network.JoinRequestDto
import com.stemtutor.core.network.MeDto
import com.stemtutor.core.network.ProfileUpdateDto
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.StudentClassroomDto

/** A StemApi where only the endpoints a test sets do anything. */
class TestStemApi(private val health: suspend () -> HealthDto = { error("not stubbed") }) : StemApi {
    override suspend fun health() = health.invoke()
    override suspend fun me(): MeDto = error("not stubbed")
    override suspend fun updateMe(body: ProfileUpdateDto): MeDto = error("not stubbed")
    override suspend fun createClassroom(body: ClassroomCreateDto): ClassroomDto = error("not stubbed")
    override suspend fun teacherClassrooms(): List<ClassroomDto> = error("not stubbed")
    override suspend fun classroom(id: String): ClassroomDetailDto = error("not stubbed")
    override suspend fun preview(key: String): ClassroomPreviewDto = error("not stubbed")
    override suspend fun join(body: JoinRequestDto): StudentClassroomDto = error("not stubbed")
    override suspend fun myClassrooms(): List<StudentClassroomDto> = error("not stubbed")
}
