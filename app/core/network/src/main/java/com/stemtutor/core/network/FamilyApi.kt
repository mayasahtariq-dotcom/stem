package com.stemtutor.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** M4: the teacher class dashboard, parent links, consent and the parent dashboard. */
interface FamilyApi {
    // ---- teacher ----
    @GET("classrooms/{id}/dashboard")
    suspend fun classDashboard(@Path("id") classroomId: String, @Query("course_id") courseId: String? = null): ClassDashboardDto

    @GET("classrooms/{id}/students/{sid}")
    suspend fun studentDetail(@Path("id") classroomId: String, @Path("sid") studentId: String): StudentDetailDto

    @PUT("classrooms/{id}/school-consent")
    suspend fun setSchoolConsent(@Path("id") classroomId: String, @Body body: SchoolConsentDto): SchoolConsentOutDto

    // ---- student ----
    @POST("me/link-code")
    suspend fun createLinkCode(@Body body: EmptyDto = EmptyDto()): LinkCodeDto

    @GET("me/parents")
    suspend fun myParents(): List<ParentDto>

    @DELETE("me/parents/{id}")
    suspend fun removeParent(@Path("id") parentId: String): Response<Unit>

    // ---- parent ----
    @GET("parent/link/{code}")
    suspend fun previewLink(@Path("code") code: String): ChildPreviewDto

    @POST("parent/link")
    suspend fun link(@Body body: LinkInDto): ChildDto

    @GET("parent/children")
    suspend fun children(): List<ChildDto>

    @POST("parent/children/{id}/consent")
    suspend fun approveConsent(@Path("id") childId: String, @Body body: EmptyDto = EmptyDto()): ChildDto

    @POST("parent/children/{id}/revoke-consent")
    suspend fun revokeConsent(@Path("id") childId: String, @Body body: EmptyDto = EmptyDto()): ChildDto

    @POST("parent/children/{id}/deletion")
    suspend fun requestDeletion(@Path("id") childId: String, @Body body: EmptyDto = EmptyDto()): DeletionDto

    @DELETE("parent/children/{id}")
    suspend fun unlink(@Path("id") childId: String): Response<Unit>

    @GET("parent/children/{id}/dashboard")
    suspend fun childDashboard(@Path("id") childId: String): ChildDashboardDto
}

@Serializable
data class TilesDto(
    @SerialName("active_this_week") val activeThisWeek: Int,
    @SerialName("student_count") val studentCount: Int,
    val accuracy: Double? = null,
    val behind: Int,
)

@Serializable
data class WeakTopicDto(
    val kind: String,
    val key: String,
    val name: Map<String, String>,
    val accuracy: Double,
    val attempts: Int,
    @SerialName("low_mastery_students") val lowMasteryStudents: Int = 0,
)

@Serializable
data class StudentRowDto(
    val id: String,
    val name: String,
    val streak: Int,
    @SerialName("current_lesson") val currentLesson: Map<String, String>? = null,
    val accuracy: Double? = null,
    @SerialName("idle_days") val idleDays: Int,
    val idle: Boolean,
    val behind: Boolean,
    val overdue: Int = 0,
)

@Serializable
data class CourseRefDto(val id: String, val title: Map<String, String>)

@Serializable
data class ClassDashboardDto(
    @SerialName("classroom_id") val classroomId: String,
    val name: String,
    @SerialName("join_key") val joinKey: String,
    @SerialName("school_consent") val schoolConsent: Boolean,
    val courses: List<CourseRefDto> = emptyList(),
    val tiles: TilesDto,
    @SerialName("weak_topics") val weakTopics: List<WeakTopicDto> = emptyList(),
    val students: List<StudentRowDto> = emptyList(),
)

@Serializable
data class LessonResultDto(
    @SerialName("lesson_id") val lessonId: String,
    val title: Map<String, String>,
    val completed: Boolean,
    val accuracy: Double? = null,
)

@Serializable
data class ConceptDto(val kind: String, val name: Map<String, String>, val mastery: Double)

@Serializable
data class ConfusionDto(val expected: Map<String, String>, val chosen: Map<String, String>, val count: Int)

@Serializable
data class StudentDetailDto(
    val id: String,
    val name: String,
    val streak: Int,
    @SerialName("last_active") val lastActive: String? = null,
    @SerialName("lessons_completed") val lessonsCompleted: Int,
    val lessons: List<LessonResultDto> = emptyList(),
    val weakest: List<ConceptDto> = emptyList(),
    val confusions: List<ConfusionDto> = emptyList(),
    val trend: String,
    @SerialName("usual_hour") val usualHour: Int? = null,
)

@Serializable
data class SchoolConsentDto(val enabled: Boolean)

@Serializable
data class SchoolConsentOutDto(@SerialName("classroom_id") val classroomId: String, @SerialName("school_consent") val schoolConsent: Boolean)

@Serializable
data class LinkCodeDto(val code: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class ParentDto(
    val id: String,
    val name: String,
    @SerialName("phone_last4") val phoneLast4: String,
    @SerialName("linked_at") val linkedAt: String,
    val consented: Boolean,
)

@Serializable
data class ClassOutDto(val id: String, val name: String, @SerialName("teacher_name") val teacherName: String)

@Serializable
data class ChildPreviewDto(val id: String, val name: String, val classes: List<ClassOutDto> = emptyList())

@Serializable
data class LinkInDto(val code: String)

@Serializable
data class ChildDto(
    val id: String,
    val name: String,
    @SerialName("consent_status") val consentStatus: String,
    @SerialName("consent_given_by_me") val consentGivenByMe: Boolean = false,
    val classes: List<ClassOutDto> = emptyList(),
)

@Serializable
data class DeletionDto(@SerialName("request_id") val requestId: String, val status: String, @SerialName("complete_by") val completeBy: String)

@Serializable
data class WeekCardDto(
    @SerialName("lessons_this_week") val lessonsThisWeek: Int,
    @SerialName("lessons_last_week") val lessonsLastWeek: Int,
    val arrow: String,
    @SerialName("minutes_this_week") val minutesThisWeek: Int,
    val streak: Int,
)

@Serializable
data class AccuracyCardDto(
    @SerialName("this_week") val thisWeek: Double? = null,
    @SerialName("last_week") val lastWeek: Double? = null,
    val word: String,
)

@Serializable
data class SubjectCardDto(val subject: String, val strongest: Map<String, String>? = null, val weakest: Map<String, String>? = null)

@Serializable
data class AssignmentDto(
    @SerialName("course_id") val courseId: String,
    val title: Map<String, String>,
    val classroom: String,
    @SerialName("due_at") val dueAt: String? = null,
    val status: String,
)

@Serializable
data class ChildDashboardDto(
    val child: ChildDto,
    @SerialName("has_activity") val hasActivity: Boolean,
    val week: WeekCardDto,
    val accuracy: AccuracyCardDto,
    val subjects: List<SubjectCardDto> = emptyList(),
    val assignments: List<AssignmentDto> = emptyList(),
    val classes: List<ClassOutDto> = emptyList(),
    val summary: Map<String, String> = emptyMap(),
)
