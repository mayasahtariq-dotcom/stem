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

/** M3 student learning endpoints: home, lesson path, playing lessons, terms, doubts and audio. */
interface LearningApi {
    @GET("me/home")
    suspend fun home(): HomeDto

    @GET("me/stats")
    suspend fun stats(): StatsDto

    @PUT("me/daily-goal")
    suspend fun setDailyGoal(@Body body: DailyGoalDto): StatsDto

    @GET("courses/{id}/path")
    suspend fun path(@Path("id") courseId: String): PathDto

    @POST("lessons/{id}/start")
    suspend fun startLesson(@Path("id") lessonId: String, @Body body: EmptyDto = EmptyDto()): StartDto

    @POST("attempts")
    suspend fun attempt(@Body body: AttemptInDto): AttemptDto

    @POST("lessons/{id}/complete")
    suspend fun completeLesson(@Path("id") lessonId: String, @Body body: EmptyDto = EmptyDto()): CompleteDto

    @GET("courses/{id}/terms")
    suspend fun terms(@Path("id") courseId: String): List<TermDto>

    @PUT("me/saved-terms/{course}/{key}")
    suspend fun saveTerm(@Path("course") courseId: String, @Path("key") key: String, @Body body: EmptyDto = EmptyDto()): Response<Unit>

    @DELETE("me/saved-terms/{course}/{key}")
    suspend fun unsaveTerm(@Path("course") courseId: String, @Path("key") key: String): Response<Unit>

    @POST("doubts")
    suspend fun doubt(@Body body: DoubtInDto): DoubtDto

    @POST("audio")
    suspend fun audio(@Body body: AudioInDto): AudioDto
}

@Serializable
data class StatsDto(
    @SerialName("xp_total") val xpTotal: Int,
    @SerialName("xp_today") val xpToday: Int,
    @SerialName("daily_goal") val dailyGoal: Int,
    val hearts: Int,
    @SerialName("max_hearts") val maxHearts: Int = 5,
    @SerialName("refill_at") val refillAt: String? = null,
    val streak: Int,
    @SerialName("longest_streak") val longestStreak: Int = 0,
)

@Serializable
data class DailyGoalDto(val goal: Int)

@Serializable
data class PathLessonDto(
    val id: String,
    val position: Int,
    val title: Map<String, String>,
    val state: String,
    @SerialName("generation_status") val generationStatus: String,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("unlocked_by") val unlockedBy: Map<String, String>? = null,
)

@Serializable
data class PathUnitDto(val id: String, val title: Map<String, String>, val lessons: List<PathLessonDto>)

@Serializable
data class PathDto(
    @SerialName("course_id") val courseId: String,
    val title: Map<String, String>,
    val subject: String,
    @SerialName("is_personal") val isPersonal: Boolean = false,
    val units: List<PathUnitDto>,
    @SerialName("done_count") val doneCount: Int,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("current_lesson_id") val currentLessonId: String? = null,
)

@Serializable
data class ContinueDto(
    @SerialName("course_id") val courseId: String,
    @SerialName("course_title") val courseTitle: Map<String, String>,
    val subject: String,
    @SerialName("lesson_id") val lessonId: String,
    @SerialName("lesson_title") val lessonTitle: Map<String, String>,
    @SerialName("lesson_number") val lessonNumber: Int,
    @SerialName("lesson_count") val lessonCount: Int,
    @SerialName("generation_status") val generationStatus: String,
    @SerialName("due_at") val dueAt: String? = null,
)

@Serializable
data class HomeClassDto(
    val id: String,
    val name: String,
    val subject: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("new_lessons") val newLessons: Int,
)

@Serializable
data class HomeCourseDto(
    val id: String,
    val title: Map<String, String>,
    val subject: String,
    @SerialName("is_personal") val isPersonal: Boolean,
    @SerialName("ai_made") val aiMade: Boolean,
    @SerialName("percent_complete") val percentComplete: Int,
    @SerialName("lesson_count") val lessonCount: Int,
)

@Serializable
data class HomeDto(
    val name: String,
    @SerialName("language_mode") val languageMode: String,
    val stats: StatsDto,
    @SerialName("continue_lesson") val continueLesson: ContinueDto? = null,
    val classes: List<HomeClassDto> = emptyList(),
    val courses: List<HomeCourseDto> = emptyList(),
)

@Serializable
data class StartDto(val lesson: LessonDto, val stats: StatsDto)

@Serializable
data class AttemptInDto(
    @SerialName("question_id") val questionId: String,
    val answer: JsonObject,
    val retry: Boolean,
    @SerialName("time_ms") val timeMs: Long,
)

@Serializable
data class AttemptDto(
    val correct: Boolean,
    @SerialName("correct_answer") val correctAnswer: JsonObject,
    val explanation: Map<String, String> = emptyMap(),
    @SerialName("xp_delta") val xpDelta: Int,
    @SerialName("heart_lost") val heartLost: Boolean,
    val stats: StatsDto,
)

@Serializable
data class CompleteDto(
    @SerialName("xp_earned") val xpEarned: Int,
    val accuracy: Double,
    val streak: Int,
    @SerialName("streak_extended") val streakExtended: Boolean,
    val stats: StatsDto,
)

@Serializable
data class TermDto(
    val key: String,
    val content: JsonObject,
    @SerialName("source_page") val sourcePage: Int,
    val saved: Boolean = false,
)

@Serializable
data class DoubtInDto(
    @SerialName("course_id") val courseId: String,
    @SerialName("question_id") val questionId: String? = null,
    @SerialName("term_key") val termKey: String? = null,
    val text: String,
    val mode: String = "ask",
)

@Serializable
data class DoubtDto(
    val id: String,
    val answer: Map<String, String>,
    @SerialName("in_notes") val inNotes: Boolean,
    @SerialName("source_pages") val sourcePages: List<Int> = emptyList(),
    @SerialName("doubts_left_today") val doubtsLeftToday: Int,
)

@Serializable
data class AudioInDto(val text: String, val language: String)

@Serializable
data class AudioDto(val url: String)
