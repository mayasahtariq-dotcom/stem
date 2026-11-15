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
import com.stemtutor.core.network.AppError
import com.stemtutor.core.network.AppException
import com.stemtutor.core.network.ApiErrorCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private fun fail(code: ApiErrorCode): Nothing = throw AppException(AppError.Api(code))

private val MITO = Tri("Mitochondria", "माइटोकॉन्ड्रिया", "ಮೈಟೊಕಾಂಡ್ರಿಯಾ")
private val NUCLEUS = Tri("Nucleus", "न्यूक्लियस", "ನ್ಯೂಕ್ಲಿಯಸ್")
private val CHLORO = Tri("Chloroplast", "क्लोरोप्लास्ट", "ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್")
private val L1 = Tri("Parts of a cell", "कोशिका के भाग", "ಜೀವಕೋಶದ ಭಾಗಗಳು")
private val L2 = Tri("Plant and animal cells", "पादप और जंतु कोशिका", "ಸಸ್ಯ ಮತ್ತು ಪ್ರಾಣಿ ಕೋಶ")
private val COURSE = Tri("The Cell", "कोशिका", "ಜೀವಕೋಶ")

/** A believable Class 9 science class of eight, for debug builds and tests. */
class FakeDashboardRepository : DashboardRepository {
    private var schoolConsent = false

    private val students = listOf(
        StudentRow("s1", "Ananya", 6, L2, 0.86f, 0, false, false, 0),
        StudentRow("s2", "Bhavya", 3, L2, 0.74f, 1, false, false, 0),
        StudentRow("s3", "Chetan", 0, L1, 0.41f, 6, true, true, 1),
        StudentRow("s4", "Divya", 2, L2, 0.69f, 0, false, false, 0),
        StudentRow("s5", "Farhan", 0, L1, null, 9, true, true, 1),
        StudentRow("s6", "Gauri", 4, L2, 0.92f, 0, false, false, 0),
        StudentRow("s7", "Harsh", 1, L1, 0.55f, 2, false, true, 1),
        StudentRow("s8", "Ishita", 5, L2, 0.81f, 1, false, false, 0),
    ).sortedWith(compareBy<StudentRow> { !it.behind }.thenBy { it.name })

    override suspend fun classDashboard(classroomId: String, courseId: String?) = Result.success(
        ClassDashboard(
            classroomId = classroomId,
            name = "Class 9-A Science",
            joinKey = "BI09-K4X2",
            schoolConsent = schoolConsent,
            courses = listOf(CourseRef("fake-course-cell", COURSE)),
            tiles = DashboardTiles(activeThisWeek = 6, studentCount = students.size, accuracy = 0.72f, behind = students.count { it.behind }),
            weakTopics = listOf(
                WeakTopic("term", CHLORO, 0.38f, 21, 4),
                WeakTopic("lesson", L1, 0.58f, 48, 3),
                WeakTopic("term", NUCLEUS, 0.66f, 30, 2),
                WeakTopic("term", MITO, 0.81f, 33, 1),
            ),
            students = students,
        ),
    )

    override suspend fun studentDetail(classroomId: String, studentId: String): Result<StudentDetail> {
        val row = students.firstOrNull { it.id == studentId } ?: return Result.failure(AppException(AppError.Api(ApiErrorCode.NOT_FOUND)))
        return Result.success(
            StudentDetail(
                id = row.id,
                name = row.name,
                streak = row.streak,
                lastActive = null,
                lessonsCompleted = if (row.currentLesson == L2) 1 else 0,
                lessons = listOf(
                    LessonResultRow("fake-lesson-1", L1, row.currentLesson == L2, row.accuracy),
                    LessonResultRow("fake-lesson-2", L2, false, null),
                ),
                weakest = listOf(Concept("term", CHLORO, 0.31f), Concept("term", NUCLEUS, 0.48f), Concept("lesson", L1, 0.55f)),
                confusions = listOf(Confusion(MITO, NUCLEUS, 3)),
                trend = if ((row.accuracy ?: 0f) >= 0.7f) Trend.IMPROVING else Trend.NEEDS_PRACTICE,
            ),
        )
    }

    override suspend fun setSchoolConsent(classroomId: String, enabled: Boolean): Result<Boolean> {
        schoolConsent = enabled
        return Result.success(enabled)
    }
}

/**
 * Parent links offline. As a student: a link code and a linked-parents list. As a parent: two
 * children (one approved, one waiting) and code [DEMO_CODE] links a third.
 */
class FakeFamilyRepository : FamilyRepository {
    private val lock = Mutex()
    private val parents = mutableListOf(LinkedParent("p1", "Meena", "4821", consented = true))
    private val classes = listOf(ChildClass("c1", "Class 9-A Science", "Mrs. Rao"))
    private val children = mutableListOf(
        Child("k1", "Ananya", ConsentStatus.APPROVED, true, classes),
        Child("k2", "Ravi", ConsentStatus.PENDING, false, listOf(ChildClass("c2", "Class 7-B Science", "Mr. Iyer"))),
    )

    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = runCatching { lock.withLock { block() } }

    private fun child(id: String) = children.firstOrNull { it.id == id } ?: fail(ApiErrorCode.NOT_FOUND)

    private fun replace(updated: Child): Child {
        children[children.indexOfFirst { it.id == updated.id }] = updated
        return updated
    }

    override suspend fun createLinkCode() = guarded { LinkCode(DEMO_CODE, "") }

    override suspend fun myParents() = guarded { parents.toList() }

    override suspend fun removeParent(parentId: String) = guarded {
        if (!parents.removeAll { it.id == parentId }) fail(ApiErrorCode.NOT_FOUND)
    }

    override suspend fun previewLink(code: String) = guarded {
        if (code.trim().uppercase() != DEMO_CODE) fail(ApiErrorCode.CODE_NOT_FOUND)
        ChildPreview("k3", "Kavya", classes)
    }

    override suspend fun link(code: String) = guarded {
        if (code.trim().uppercase() != DEMO_CODE) fail(ApiErrorCode.CODE_NOT_FOUND)
        children.firstOrNull { it.id == "k3" } ?: Child("k3", "Kavya", ConsentStatus.PENDING, false, classes).also { children += it }
    }

    override suspend fun children() = guarded { children.toList() }

    override suspend fun approveConsent(childId: String) = guarded {
        replace(child(childId).copy(consent = ConsentStatus.APPROVED, consentGivenByMe = true))
    }

    override suspend fun revokeConsent(childId: String) = guarded {
        replace(child(childId).copy(consent = ConsentStatus.REVOKED, consentGivenByMe = false))
    }

    override suspend fun requestDeletion(childId: String) = guarded {
        child(childId)
        children.removeAll { it.id == childId }
        Unit
    }

    override suspend fun unlink(childId: String) = guarded {
        child(childId)
        children.removeAll { it.id == childId }
        Unit
    }

    override suspend fun childDashboard(childId: String) = guarded {
        val c = child(childId)
        if (c.id != "k1") {
            ChildDashboard(
                child = c,
                hasActivity = false,
                week = WeekCard(0, 0, Arrow.SAME, 0, 0),
                accuracy = AccuracyCard(null, null, Trend.STEADY),
                subjects = emptyList(),
                assignments = listOf(Assignment("fake-course-cell", COURSE, c.classes.first().name, null, AssignmentStatus.PENDING)),
                classes = c.classes,
                summary = Tri(
                    "${c.name} hasn't finished a lesson this week yet. Their accuracy is steady. 1 assignments are due soon.",
                    "${c.name} ने इस हफ़्ते अभी तक कोई पाठ पूरा नहीं किया है। सही जवाबों की दर स्थिर है। 1 असाइनमेंट जल्द पूरे करने हैं।",
                    "${c.name} ಈ ವಾರ ಇನ್ನೂ ಯಾವ ಪಾಠವನ್ನೂ ಮುಗಿಸಿಲ್ಲ. ಸರಿ ಉತ್ತರಗಳ ಪ್ರಮಾಣ ಸ್ಥಿರವಾಗಿದೆ. 1 ಅಸೈನ್‌ಮೆಂಟ್‌ಗಳನ್ನು ಶೀಘ್ರದಲ್ಲೇ ಮುಗಿಸಬೇಕು.",
                ),
            )
        } else {
            ChildDashboard(
                child = c,
                hasActivity = true,
                week = WeekCard(4, 2, Arrow.UP, 38, 6),
                accuracy = AccuracyCard(0.84f, 0.71f, Trend.IMPROVING),
                subjects = listOf(SubjectCard(Subject.BIOLOGY, MITO, CHLORO)),
                assignments = listOf(
                    Assignment("fake-course-cell", COURSE, "Class 9-A Science", "2026-11-20T18:30:00Z", AssignmentStatus.DONE),
                    Assignment("fake-course-light", Tri("Light", "प्रकाश", "ಬೆಳಕು"), "Class 9-A Science", "2026-11-12T18:30:00Z", AssignmentStatus.OVERDUE),
                ),
                classes = c.classes,
                summary = Tri(
                    "Ananya finished 4 lessons this week, more than last week. Their accuracy is improving; Chloroplast needs more practice. 1 assignments are overdue.",
                    "Ananya ने इस हफ़्ते 4 पाठ पूरे किए, पिछले हफ़्ते से ज़्यादा। सही जवाबों की दर सुधर रही है; क्लोरोप्लास्ट पर और अभ्यास चाहिए। 1 असाइनमेंट की तारीख निकल गई है।",
                    "Ananya ಈ ವಾರ 4 ಪಾಠಗಳನ್ನು ಮುಗಿಸಿದರು, ಕಳೆದ ವಾರಕ್ಕಿಂತ ಹೆಚ್ಚು. ಸರಿ ಉತ್ತರಗಳ ಪ್ರಮಾಣ ಸುಧಾರಿಸುತ್ತಿದೆ; ಕ್ಲೋರೋಪ್ಲಾಸ್ಟ್ ಮೇಲೆ ಇನ್ನಷ್ಟು ಅಭ್ಯಾಸ ಬೇಕು. 1 ಅಸೈನ್‌ಮೆಂಟ್‌ಗಳ ಗಡುವು ಮುಗಿದಿದೆ.",
                ),
            )
        }
    }

    companion object {
        const val DEMO_CODE = "KAV234"
    }
}
