package com.stemtutor.core.data.health

import com.stemtutor.core.network.AssignDto
import com.stemtutor.core.network.ClassroomCreateDto
import com.stemtutor.core.network.CourseDto
import com.stemtutor.core.network.CourseSummaryDto
import com.stemtutor.core.network.EmptyDto
import com.stemtutor.core.network.LessonDto
import com.stemtutor.core.network.LessonSummaryDto
import com.stemtutor.core.network.PublishDto
import com.stemtutor.core.network.QuestionDto
import com.stemtutor.core.network.QuestionUpdateDto
import com.stemtutor.core.network.TitleUpdateDto
import com.stemtutor.core.network.UploadCreateDto
import com.stemtutor.core.network.UploadCreatedDto
import com.stemtutor.core.network.UploadStatusDto
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
    override suspend fun createUpload(body: UploadCreateDto): UploadCreatedDto = error("not stubbed")
    override suspend fun startUpload(id: String, body: EmptyDto): UploadStatusDto = error("not stubbed")
    override suspend fun uploadStatus(id: String): UploadStatusDto = error("not stubbed")
    override suspend fun teacherCourses(): List<CourseSummaryDto> = error("not stubbed")
    override suspend fun studentCourses(): List<CourseSummaryDto> = error("not stubbed")
    override suspend fun course(id: String): CourseDto = error("not stubbed")
    override suspend fun updateCourseTitle(id: String, body: TitleUpdateDto): CourseDto = error("not stubbed")
    override suspend fun lesson(id: String): LessonDto = error("not stubbed")
    override suspend fun updateQuestion(id: String, body: QuestionUpdateDto): QuestionDto = error("not stubbed")
    override suspend fun regenerateQuestion(id: String, body: EmptyDto): QuestionDto = error("not stubbed")
    override suspend fun deleteQuestion(id: String): retrofit2.Response<Unit> = error("not stubbed")
    override suspend fun publishLesson(id: String, body: PublishDto): LessonSummaryDto = error("not stubbed")
    override suspend fun assignCourse(id: String, body: AssignDto): CourseDto = error("not stubbed")
}
