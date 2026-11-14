package com.stemtutor.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
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
