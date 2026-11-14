package com.stemtutor.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** The FastAPI backend. Every data read and write goes through here, never to tables directly. */
interface StemApi {
    @GET("health")
    suspend fun health(): HealthDto

    @GET("me")
    suspend fun me(): MeDto

    @PUT("me")
    suspend fun updateMe(@Body body: ProfileUpdateDto): MeDto

    @POST("classrooms")
    suspend fun createClassroom(@Body body: ClassroomCreateDto): ClassroomDto

    @GET("classrooms")
    suspend fun teacherClassrooms(): List<ClassroomDto>

    @GET("classrooms/{id}")
    suspend fun classroom(@Path("id") id: String): ClassroomDetailDto

    @GET("classrooms/preview/{key}")
    suspend fun preview(@Path("key") key: String): ClassroomPreviewDto

    @POST("classrooms/join")
    suspend fun join(@Body body: JoinRequestDto): StudentClassroomDto

    @GET("me/classrooms")
    suspend fun myClassrooms(): List<StudentClassroomDto>

    // ---- M2: notes to course ----

    @POST("uploads")
    suspend fun createUpload(@Body body: UploadCreateDto): UploadCreatedDto

    @POST("uploads/{id}/start")
    suspend fun startUpload(@Path("id") id: String, @Body body: EmptyDto = EmptyDto()): UploadStatusDto

    @GET("uploads/{id}")
    suspend fun uploadStatus(@Path("id") id: String): UploadStatusDto

    @GET("courses")
    suspend fun teacherCourses(): List<CourseSummaryDto>

    @GET("me/courses")
    suspend fun studentCourses(): List<CourseSummaryDto>

    @GET("courses/{id}")
    suspend fun course(@Path("id") id: String): CourseDto

    @PUT("courses/{id}")
    suspend fun updateCourseTitle(@Path("id") id: String, @Body body: TitleUpdateDto): CourseDto

    @GET("lessons/{id}")
    suspend fun lesson(@Path("id") id: String): LessonDto

    @PUT("questions/{id}")
    suspend fun updateQuestion(@Path("id") id: String, @Body body: QuestionUpdateDto): QuestionDto

    @POST("questions/{id}/regenerate")
    suspend fun regenerateQuestion(@Path("id") id: String, @Body body: EmptyDto = EmptyDto()): QuestionDto

    @DELETE("questions/{id}")
    suspend fun deleteQuestion(@Path("id") id: String): Response<Unit>

    @POST("lessons/{id}/publish")
    suspend fun publishLesson(@Path("id") id: String, @Body body: PublishDto): LessonSummaryDto

    @POST("courses/{id}/assign")
    suspend fun assignCourse(@Path("id") id: String, @Body body: AssignDto): CourseDto
}

@Serializable
data class HealthDto(val status: String, val version: String)

@Serializable
data class MeDto(
    val id: String,
    val registered: Boolean = false,
    val name: String? = null,
    val phone: String? = null,
    val roles: List<String> = emptyList(),
    @SerialName("language_mode") val languageMode: String? = null,
)

@Serializable
data class ProfileUpdateDto(
    val name: String,
    @SerialName("language_mode") val languageMode: String,
    val roles: List<String>,
)

@Serializable
data class ClassroomCreateDto(
    val name: String,
    val subject: String,
    val grade: Int,
    @SerialName("allowed_language_modes") val allowedLanguageModes: List<String>,
)

@Serializable
data class ClassroomDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: Int,
    @SerialName("join_key") val joinKey: String,
    @SerialName("allowed_language_modes") val allowedLanguageModes: List<String>,
    @SerialName("student_count") val studentCount: Int,
)

@Serializable
data class MemberDto(val id: String, val name: String, @SerialName("joined_at") val joinedAt: String)

@Serializable
data class ClassroomDetailDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: Int,
    @SerialName("join_key") val joinKey: String,
    @SerialName("allowed_language_modes") val allowedLanguageModes: List<String>,
    @SerialName("student_count") val studentCount: Int,
    val members: List<MemberDto>,
)

@Serializable
data class ClassroomPreviewDto(
    val id: String,
    val name: String,
    @SerialName("teacher_name") val teacherName: String,
    val subject: String,
    val grade: Int,
    @SerialName("student_count") val studentCount: Int,
    @SerialName("allowed_language_modes") val allowedLanguageModes: List<String>,
)

@Serializable
data class JoinRequestDto(val key: String)

@Serializable
data class StudentClassroomDto(
    val id: String,
    val name: String,
    @SerialName("teacher_name") val teacherName: String,
    val subject: String,
    val grade: Int,
    @SerialName("student_count") val studentCount: Int,
)

// ---- M2 DTOs ----

@Serializable
class EmptyDto

@Serializable
data class PageInDto(
    val kind: String,
    val text: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
)

@Serializable
data class UploadCreateDto(
    val pages: List<PageInDto>,
    val subject: String? = null,
    val grade: Int? = null,
    @SerialName("classroom_id") val classroomId: String? = null,
    val personal: Boolean = false,
)

@Serializable
data class PageTargetDto(
    val index: Int,
    @SerialName("upload_url") val uploadUrl: String? = null,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
)

@Serializable
data class UploadCreatedDto(val id: String, val pages: List<PageTargetDto>)

@Serializable
data class UploadStatusDto(
    val id: String,
    val status: String,
    val steps: List<String> = emptyList(),
    @SerialName("page_count") val pageCount: Int = 0,
    val error: String? = null,
    @SerialName("course_id") val courseId: String? = null,
    @SerialName("first_lesson_id") val firstLessonId: String? = null,
)

@Serializable
data class CourseSummaryDto(
    val id: String,
    val title: Map<String, String>,
    val subject: String,
    val grade: Int,
    val status: String,
    @SerialName("is_personal") val isPersonal: Boolean,
    @SerialName("ai_made") val aiMade: Boolean,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("published_count") val publishedCount: Int,
)

@Serializable
data class LessonSummaryDto(
    val id: String,
    val position: Int,
    val title: Map<String, String>,
    @SerialName("generation_status") val generationStatus: String,
    val published: Boolean,
    @SerialName("question_count") val questionCount: Int,
    @SerialName("flagged_count") val flaggedCount: Int = 0,
)

@Serializable
data class UnitDto(val id: String, val title: Map<String, String>, val lessons: List<LessonSummaryDto>)

@Serializable
data class CourseDto(
    val id: String,
    val title: Map<String, String>,
    val subject: String,
    val grade: Int,
    val status: String,
    @SerialName("is_personal") val isPersonal: Boolean,
    @SerialName("ai_made") val aiMade: Boolean,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("published_count") val publishedCount: Int,
    val units: List<UnitDto>,
    @SerialName("assigned_classroom_ids") val assignedClassroomIds: List<String> = emptyList(),
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("edited_count") val editedCount: Int = 0,
)

@Serializable
data class TitleUpdateDto(val title: Map<String, String>)

@Serializable
data class QuestionDto(
    val id: String,
    val position: Int,
    val type: String,
    val content: JsonObject,
    @SerialName("source_page") val sourcePage: Int,
    val confidence: Double,
    @SerialName("flag_reason") val flagReason: String? = null,
    @SerialName("call_safe") val callSafe: Boolean = true,
    @SerialName("teacher_edited") val teacherEdited: Boolean = false,
)

@Serializable
data class LessonDto(
    val id: String,
    @SerialName("course_id") val courseId: String,
    val title: Map<String, String>,
    @SerialName("generation_status") val generationStatus: String,
    val published: Boolean,
    val questions: List<QuestionDto>,
    val terms: Map<String, JsonObject> = emptyMap(),
)

@Serializable
data class QuestionUpdateDto(val question: JsonObject)

@Serializable
data class PublishDto(@SerialName("classroom_id") val classroomId: String? = null)

@Serializable
data class AssignDto(
    @SerialName("classroom_ids") val classroomIds: List<String>,
    @SerialName("due_at") val dueAt: String? = null,
)
