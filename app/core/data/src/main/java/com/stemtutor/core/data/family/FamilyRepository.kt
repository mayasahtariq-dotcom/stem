package com.stemtutor.core.data.family

import com.stemtutor.core.data.model.AccuracyCard
import com.stemtutor.core.data.model.Arrow
import com.stemtutor.core.data.model.Assignment
import com.stemtutor.core.data.model.AssignmentStatus
import com.stemtutor.core.data.model.Child
import com.stemtutor.core.data.model.ChildClass
import com.stemtutor.core.data.model.ChildDashboard
import com.stemtutor.core.data.model.ChildPreview
import com.stemtutor.core.data.model.ClassDashboard
import com.stemtutor.core.data.model.Concept
import com.stemtutor.core.data.model.Confusion
import com.stemtutor.core.data.model.ConsentStatus
import com.stemtutor.core.data.model.CourseRef
import com.stemtutor.core.data.model.DashboardTiles
import com.stemtutor.core.data.model.LessonResultRow
import com.stemtutor.core.data.model.LinkCode
import com.stemtutor.core.data.model.LinkedParent
import com.stemtutor.core.data.model.StudentDetail
import com.stemtutor.core.data.model.StudentRow
import com.stemtutor.core.data.model.Subject
import com.stemtutor.core.data.model.SubjectCard
import com.stemtutor.core.data.model.Trend
import com.stemtutor.core.data.model.Tri
import com.stemtutor.core.data.model.WeakTopic
import com.stemtutor.core.data.model.WeekCard
import com.stemtutor.core.network.ChildDashboardDto
import com.stemtutor.core.network.ChildDto
import com.stemtutor.core.network.ClassOutDto
import com.stemtutor.core.network.FamilyApi
import com.stemtutor.core.network.LinkInDto
import com.stemtutor.core.network.SchoolConsentDto
import com.stemtutor.core.network.apiCall

/** The teacher's class dashboard (M4). */
interface DashboardRepository {
    suspend fun classDashboard(classroomId: String, courseId: String? = null): Result<ClassDashboard>

    suspend fun studentDetail(classroomId: String, studentId: String): Result<StudentDetail>

    suspend fun setSchoolConsent(classroomId: String, enabled: Boolean): Result<Boolean>
}

/** Parent links and consent: the student side (code, parents) and the parent side (M4). */
interface FamilyRepository {
    suspend fun createLinkCode(): Result<LinkCode>

    suspend fun myParents(): Result<List<LinkedParent>>

    suspend fun removeParent(parentId: String): Result<Unit>

    suspend fun previewLink(code: String): Result<ChildPreview>

    suspend fun link(code: String): Result<Child>

    suspend fun children(): Result<List<Child>>

    suspend fun approveConsent(childId: String): Result<Child>

    suspend fun revokeConsent(childId: String): Result<Child>

    suspend fun requestDeletion(childId: String): Result<Unit>

    suspend fun unlink(childId: String): Result<Unit>

    suspend fun childDashboard(childId: String): Result<ChildDashboard>
}

private fun ClassOutDto.toModel() = ChildClass(id, name, teacherName)

internal fun ChildDto.toModel() = Child(id, name, ConsentStatus.fromApi(consentStatus), consentGivenByMe, classes.map { it.toModel() })

internal fun ChildDashboardDto.toModel() = ChildDashboard(
    child = child.toModel(),
    hasActivity = hasActivity,
    week = WeekCard(
        week.lessonsThisWeek,
        week.lessonsLastWeek,
        when (week.arrow) {
            "up" -> Arrow.UP
            "down" -> Arrow.DOWN
            else -> Arrow.SAME
        },
        week.minutesThisWeek,
        week.streak,
    ),
    accuracy = AccuracyCard(accuracy.thisWeek?.toFloat(), accuracy.lastWeek?.toFloat(), Trend.fromApi(accuracy.word)),
    subjects = subjects.map { SubjectCard(Subject.fromCode(it.subject), it.strongest?.let(Tri::of), it.weakest?.let(Tri::of)) },
    assignments = assignments.map {
        Assignment(
            it.courseId,
            Tri.of(it.title),
            it.classroom,
            it.dueAt,
            when (it.status) {
                "done" -> AssignmentStatus.DONE
                "overdue" -> AssignmentStatus.OVERDUE
                else -> AssignmentStatus.PENDING
            },
        )
    },
    classes = classes.map { it.toModel() },
    summary = Tri.of(summary),
)

class RemoteDashboardRepository(private val api: FamilyApi) : DashboardRepository {
    override suspend fun classDashboard(classroomId: String, courseId: String?) = apiCall {
        val d = api.classDashboard(classroomId, courseId)
        ClassDashboard(
            classroomId = d.classroomId,
            name = d.name,
            joinKey = d.joinKey,
            schoolConsent = d.schoolConsent,
            courses = d.courses.map { CourseRef(it.id, Tri.of(it.title)) },
            tiles = DashboardTiles(d.tiles.activeThisWeek, d.tiles.studentCount, d.tiles.accuracy?.toFloat(), d.tiles.behind),
            weakTopics = d.weakTopics.map { WeakTopic(it.kind, Tri.of(it.name), it.accuracy.toFloat(), it.attempts, it.lowMasteryStudents) },
            students = d.students.map {
                StudentRow(it.id, it.name, it.streak, it.currentLesson?.let(Tri::of), it.accuracy?.toFloat(), it.idleDays, it.idle, it.behind, it.overdue)
            },
        )
    }

    override suspend fun studentDetail(classroomId: String, studentId: String) = apiCall {
        val d = api.studentDetail(classroomId, studentId)
        StudentDetail(
            id = d.id,
            name = d.name,
            streak = d.streak,
            lastActive = d.lastActive,
            lessonsCompleted = d.lessonsCompleted,
            lessons = d.lessons.map { LessonResultRow(it.lessonId, Tri.of(it.title), it.completed, it.accuracy?.toFloat()) },
            weakest = d.weakest.map { Concept(it.kind, Tri.of(it.name), it.mastery.toFloat()) },
            confusions = d.confusions.map { Confusion(Tri.of(it.expected), Tri.of(it.chosen), it.count) },
            trend = Trend.fromApi(d.trend),
        )
    }

    override suspend fun setSchoolConsent(classroomId: String, enabled: Boolean) = apiCall {
        api.setSchoolConsent(classroomId, SchoolConsentDto(enabled)).schoolConsent
    }
}

class RemoteFamilyRepository(private val api: FamilyApi) : FamilyRepository {
    override suspend fun createLinkCode() = apiCall { api.createLinkCode().let { LinkCode(it.code, it.expiresAt) } }

    override suspend fun myParents() = apiCall { api.myParents().map { LinkedParent(it.id, it.name, it.phoneLast4, it.consented) } }

    override suspend fun removeParent(parentId: String) = apiCall {
        api.removeParent(parentId)
        Unit
    }

    override suspend fun previewLink(code: String) = apiCall {
        api.previewLink(code.trim().uppercase()).let { p -> ChildPreview(p.id, p.name, p.classes.map { ChildClass(it.id, it.name, it.teacherName) }) }
    }

    override suspend fun link(code: String) = apiCall { api.link(LinkInDto(code.trim().uppercase())).toModel() }

    override suspend fun children() = apiCall { api.children().map { it.toModel() } }

    override suspend fun approveConsent(childId: String) = apiCall { api.approveConsent(childId).toModel() }

    override suspend fun revokeConsent(childId: String) = apiCall { api.revokeConsent(childId).toModel() }

    override suspend fun requestDeletion(childId: String) = apiCall {
        api.requestDeletion(childId)
        Unit
    }

    override suspend fun unlink(childId: String) = apiCall {
        api.unlink(childId)
        Unit
    }

    override suspend fun childDashboard(childId: String) = apiCall { api.childDashboard(childId).toModel() }
}
