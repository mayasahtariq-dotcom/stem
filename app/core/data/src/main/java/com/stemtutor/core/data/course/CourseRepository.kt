package com.stemtutor.core.data.course

import com.stemtutor.core.data.model.CourseDetail
import com.stemtutor.core.data.model.CourseSummary
import com.stemtutor.core.data.model.CourseUnit
import com.stemtutor.core.data.model.GenerationStatus
import com.stemtutor.core.data.model.LessonDetail
import com.stemtutor.core.data.model.LessonQuestion
import com.stemtutor.core.data.model.LessonSummary
import com.stemtutor.core.data.model.NewUpload
import com.stemtutor.core.data.model.PageKind
import com.stemtutor.core.data.model.PipelineStep
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.model.UploadProgress
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.AssignDto
import com.stemtutor.core.network.CourseDto
import com.stemtutor.core.network.CourseSummaryDto
import com.stemtutor.core.network.LessonSummaryDto
import com.stemtutor.core.network.PageInDto
import com.stemtutor.core.network.PublishDto
import com.stemtutor.core.network.QuestionUpdateDto
import com.stemtutor.core.network.StemApi
import com.stemtutor.core.network.TitleUpdateDto
import com.stemtutor.core.network.UploadCreateDto
import com.stemtutor.core.network.UploadStatusDto
import com.stemtutor.core.network.apiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

interface CourseRepository {
    /** Creates the upload, sends every page to storage, and starts the pipeline. */
    suspend fun createAndStart(upload: NewUpload): Result<UploadProgress>

    suspend fun progress(uploadId: String): Result<UploadProgress>

    suspend fun teacherCourses(): Result<List<CourseSummary>>

    suspend fun studentCourses(): Result<List<CourseSummary>>

    suspend fun course(id: String): Result<CourseDetail>

    suspend fun renameCourse(id: String, title: Tri): Result<CourseDetail>

    suspend fun lesson(id: String): Result<LessonDetail>

    suspend fun saveQuestion(question: LessonQuestion): Result<LessonQuestion>

    suspend fun regenerateQuestion(id: String): Result<LessonQuestion>

    suspend fun deleteQuestion(id: String): Result<Unit>

    suspend fun publishLesson(lessonId: String, classroomId: String?): Result<Unit>

    /** [dueAtIso] like 2026-12-01T00:00:00Z, or null for no due date. */
    suspend fun assignCourse(courseId: String, classroomIds: List<String>, dueAtIso: String?): Result<CourseDetail>
}

internal fun UploadStatusDto.toModel() = UploadProgress(
    id = id,
    status = status,
    stepsDone = steps.mapNotNull(PipelineStep::fromApi),
    pageCount = pageCount,
    error = error,
    courseId = courseId,
    firstLessonId = firstLessonId,
)

internal fun CourseSummaryDto.toModel() = CourseSummary(
    id, Tri.of(title), Subject.fromCode(subject), grade, status, isPersonal, aiMade, lessonCount, publishedCount,
)

internal fun LessonSummaryDto.toModel() = LessonSummary(
    id, position, Tri.of(title), GenerationStatus.fromApi(generationStatus), published, questionCount, flaggedCount,
)

internal fun CourseDto.toModel() = CourseDetail(
    summary = CourseSummary(
        id, Tri.of(title), Subject.fromCode(subject), grade, status, isPersonal, aiMade, lessonCount, publishedCount,
    ),
    units = units.map { u -> CourseUnit(u.id, Tri.of(u.title), u.lessons.map { it.toModel() }) },
    assignedClassroomIds = assignedClassroomIds,
    questionCount = questionCount,
    editedCount = editedCount,
)

class RemoteCourseRepository(private val api: StemApi) : CourseRepository {
    // Page files go straight to storage via signed links, without our API token.
    private val files = OkHttpClient.Builder().writeTimeout(60, TimeUnit.SECONDS).build()

    override suspend fun createAndStart(upload: NewUpload): Result<UploadProgress> = apiCall {
        val created = api.createUpload(
            UploadCreateDto(
                pages = upload.pages.map { PageInDto(it.kind.apiValue, it.text, it.bytes?.size?.toLong()) },
                subject = upload.subject?.code,
                grade = upload.grade,
                classroomId = upload.classroomId,
                personal = upload.personal,
            ),
        )
        created.pages.forEach { target ->
            val page = upload.pages[target.index]
            val url = target.uploadUrl ?: return@forEach
            val type = if (page.kind == PageKind.PDF) "application/pdf" else "image/jpeg"
            val request = Request.Builder()
                .url(url)
                .method(target.method, requireNotNull(page.bytes).toRequestBody(type.toMediaType()))
                .apply { target.headers.forEach { (k, v) -> header(k, v) } }
                .build()
            withContext(Dispatchers.IO) {
                files.newCall(request).execute().use { if (!it.isSuccessful) throw IOException("upload failed: ${it.code}") }
            }
        }
        api.startUpload(created.id).toModel()
    }

    override suspend fun progress(uploadId: String) = apiCall { api.uploadStatus(uploadId).toModel() }

    override suspend fun teacherCourses() = apiCall { api.teacherCourses().map { it.toModel() } }

    override suspend fun studentCourses() = apiCall { api.studentCourses().map { it.toModel() } }

    override suspend fun course(id: String) = apiCall { api.course(id).toModel() }

    override suspend fun renameCourse(id: String, title: Tri) = apiCall {
        api.updateCourseTitle(id, TitleUpdateDto(mapOf("en" to title.en, "hi" to title.hi, "kn" to title.kn))).toModel()
    }

    override suspend fun lesson(id: String) = apiCall {
        val dto = api.lesson(id)
        LessonDetail(
            id = dto.id,
            courseId = dto.courseId,
            title = Tri.of(dto.title),
            generationStatus = GenerationStatus.fromApi(dto.generationStatus),
            published = dto.published,
            questions = dto.questions.map { QuestionCodec.decode(it, dto.terms) },
        )
    }

    override suspend fun saveQuestion(question: LessonQuestion) = apiCall {
        QuestionCodec.decode(api.updateQuestion(question.id, QuestionUpdateDto(QuestionCodec.encode(question))), emptyMap())
    }

    override suspend fun regenerateQuestion(id: String) = apiCall {
        QuestionCodec.decode(api.regenerateQuestion(id), emptyMap())
    }

    override suspend fun deleteQuestion(id: String) = apiCall {
        val r = api.deleteQuestion(id)
        if (!r.isSuccessful) throw AppException(AppError.Unknown)
    }

    override suspend fun publishLesson(lessonId: String, classroomId: String?) = apiCall {
        api.publishLesson(lessonId, PublishDto(classroomId))
        Unit
    }

    override suspend fun assignCourse(courseId: String, classroomIds: List<String>, dueAtIso: String?) = apiCall {
        api.assignCourse(courseId, AssignDto(classroomIds, dueAtIso)).toModel()
    }
}
