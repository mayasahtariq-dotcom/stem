"""Teacher class dashboard (PRD M4): tiles, weak topics, the students list and a student's
detail, plus the "school has collected consent" switch. Numbers come from answers and progress
on the class's courses; the Student Twin adds weakest concepts and confusions.

The teacher's ownership of the classroom is checked through their own session (RLS); the
class-wide numbers are then read with the service session and cached for five minutes.
"""

import time
import uuid
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import UTC, datetime, timedelta
from typing import Annotated, Any

from fastapi import APIRouter, Depends
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.ai.providers import ServicesDep
from app.auth import CurrentUser, get_current_user
from app.db.models import (
    AnswerAttempt,
    Classroom,
    Course,
    CourseAssignment,
    Lesson,
    LessonProgress,
    Membership,
    QuestionRow,
    StudentStats,
    TermRow,
    User,
)
from app.db.session import ServiceDb, UserDb
from app.errors import ErrorCode, api_error
from app.learning import rules
from app.twin import model as twin_model
from app.twin import service as twin

router = APIRouter(tags=["dashboard"])
Me = Annotated[CurrentUser, Depends(get_current_user)]

IDLE_DAYS = 3
WEAK_TOPICS = 5


def now() -> datetime:
    """The current time; tests replace it."""
    return datetime.now(UTC)


# ---- shared calculations -------------------------------------------------------------------


@dataclass
class ClassData:
    """Everything the dashboard needs about one classroom, loaded once."""

    classroom: Classroom
    students: list[tuple[User, datetime]]  # (student, joined_at)
    courses: list[Course]
    assignments: dict[uuid.UUID, datetime | None]  # course -> due date
    lessons: list[Lesson]  # published, in course order
    attempts: list[AnswerAttempt]
    questions: dict[uuid.UUID, QuestionRow]
    progress: dict[tuple[uuid.UUID, uuid.UUID], LessonProgress]  # (student, lesson)
    stats: dict[uuid.UUID, StudentStats]
    terms: dict[uuid.UUID, dict[str, dict[str, Any]]] = field(default_factory=dict)

    @property
    def lesson_course(self) -> dict[uuid.UUID, uuid.UUID]:
        return {lesson.id: lesson.course_id for lesson in self.lessons}


def own_classroom(db: Session, classroom_id: uuid.UUID, user_id: str) -> Classroom:
    c = db.scalar(
        select(Classroom).where(
            Classroom.id == classroom_id, Classroom.teacher_id == uuid.UUID(user_id)
        )
    )
    if c is None:
        raise api_error(ErrorCode.NOT_FOUND)
    return c


def load_class(db: Session, classroom: Classroom, course_id: uuid.UUID | None) -> ClassData:
    students = [
        (u, joined)
        for u, joined in db.execute(
            select(User, Membership.joined_at)
            .join(Membership, Membership.student_id == User.id)
            .where(Membership.classroom_id == classroom.id, Membership.status == "active")
        ).all()
    ]
    assignments = {
        a.course_id: a.due_at
        for a in db.scalars(
            select(CourseAssignment).where(CourseAssignment.classroom_id == classroom.id)
        )
    }
    course_ids = [c for c in assignments if course_id is None or c == course_id]
    courses = (
        list(db.scalars(select(Course).where(Course.id.in_(course_ids)))) if course_ids else []
    )
    lessons = (
        list(
            db.scalars(
                select(Lesson)
                .where(Lesson.course_id.in_(course_ids), Lesson.published_at.is_not(None))
                .order_by(Lesson.course_id, Lesson.position)
            )
        )
        if course_ids
        else []
    )
    student_ids = [u.id for u, _ in students]
    lesson_ids = [lesson.id for lesson in lessons]
    attempts = (
        list(
            db.scalars(
                select(AnswerAttempt).where(
                    AnswerAttempt.student_id.in_(student_ids),
                    AnswerAttempt.lesson_id.in_(lesson_ids),
                )
            )
        )
        if student_ids and lesson_ids
        else []
    )
    questions = (
        {
            q.id: q
            for q in db.scalars(select(QuestionRow).where(QuestionRow.lesson_id.in_(lesson_ids)))
        }
        if lesson_ids
        else {}
    )
    progress = (
        {
            (p.student_id, p.lesson_id): p
            for p in db.scalars(
                select(LessonProgress).where(
                    LessonProgress.student_id.in_(student_ids),
                    LessonProgress.lesson_id.in_(lesson_ids),
                )
            )
        }
        if student_ids and lesson_ids
        else {}
    )
    stats = (
        {
            s.student_id: s
            for s in db.scalars(
                select(StudentStats).where(StudentStats.student_id.in_(student_ids))
            )
        }
        if student_ids
        else {}
    )
    terms: dict[uuid.UUID, dict[str, dict[str, Any]]] = defaultdict(dict)
    if course_ids:
        for t in db.scalars(select(TermRow).where(TermRow.course_id.in_(course_ids))):
            terms[t.course_id][t.key] = t.content
    return ClassData(
        classroom,
        students,
        courses,
        {c: assignments[c] for c in course_ids},
        lessons,
        attempts,
        questions,
        progress,
        stats,
        dict(terms),
    )


def accuracy(attempts: list[AnswerAttempt]) -> float | None:
    """First-try correct answers ÷ first-try attempts."""
    first = [a for a in attempts if a.first_try]
    return sum(a.correct for a in first) / len(first) if first else None


def last_active(data: ClassData, student: User, joined: datetime) -> datetime:
    times = [a.created_at for a in data.attempts if a.student_id == student.id]
    times += [
        p.completed_at
        for (sid, _), p in data.progress.items()
        if sid == student.id and p.completed_at is not None
    ]
    return max(times) if times else joined


def overdue_courses(data: ClassData, student_id: uuid.UUID, at: datetime) -> list[Course]:
    """Assigned courses past their due date that the student hasn't finished."""
    out = []
    for course in data.courses:
        due = data.assignments.get(course.id)
        if due is None or due >= at:
            continue
        lessons = [ls for ls in data.lessons if ls.course_id == course.id]
        done = all(
            (p := data.progress.get((student_id, ls.id))) is not None and p.status == "completed"
            for ls in lessons
        )
        if lessons and not done:
            out.append(course)
    return out


def current_lesson(data: ClassData, student_id: uuid.UUID) -> Lesson | None:
    for lesson in data.lessons:
        p = data.progress.get((student_id, lesson.id))
        if p is None or p.status != "completed":
            return lesson
    return None


# ---- endpoints -----------------------------------------------------------------------------


class Tiles(BaseModel):
    active_this_week: int
    student_count: int
    accuracy: float | None
    behind: int


class WeakTopic(BaseModel):
    kind: str
    key: str
    name: dict[str, str]
    accuracy: float
    attempts: int
    low_mastery_students: int


class StudentRow(BaseModel):
    id: str
    name: str
    streak: int
    current_lesson: dict[str, str] | None
    accuracy: float | None
    idle_days: int
    idle: bool
    behind: bool
    overdue: int


class CourseRef(BaseModel):
    id: str
    title: dict[str, str]


class DashboardOut(BaseModel):
    classroom_id: str
    name: str
    join_key: str
    school_consent: bool
    courses: list[CourseRef]
    tiles: Tiles
    weak_topics: list[WeakTopic]
    students: list[StudentRow]


_cache: dict[tuple[str, str | None], tuple[float, DashboardOut]] = {}


def _concept_accuracy(data: ClassData) -> dict[tuple[str, str], list[bool]]:
    """First-try results per lesson and per term (terms found as the twin finds them)."""
    results: dict[tuple[str, str], list[bool]] = defaultdict(list)
    lesson_course = data.lesson_course
    for a in data.attempts:
        q = data.questions.get(a.question_id)
        if q is None or not a.first_try:
            continue
        ev = twin_model.evidence(
            str(a.lesson_id),
            q.type,
            q.content,
            a.answer,
            a.correct,
            data.terms.get(lesson_course.get(a.lesson_id, uuid.UUID(int=0)), {}),
        )
        for concept in ev.concepts:
            results[concept].append(a.correct)
    return results


def build_dashboard(
    db: Session, classroom: Classroom, course_id: uuid.UUID | None, at: datetime
) -> DashboardOut:
    data = load_class(db, classroom, course_id)
    week_ago = at - timedelta(days=7)
    today = rules.india_today(at)

    recent = [a for a in data.attempts if a.created_at >= week_ago]
    active = {
        sid
        for (sid, _), p in data.progress.items()
        if p.completed_at is not None and p.completed_at >= week_ago
    }

    rows: list[StudentRow] = []
    for student, joined in data.students:
        idle_days = (at - last_active(data, student, joined)).days
        overdue = overdue_courses(data, student.id, at)
        idle = idle_days > IDLE_DAYS
        st = data.stats.get(student.id)
        cur = current_lesson(data, student.id)
        rows.append(
            StudentRow(
                id=str(student.id),
                name=student.name,
                streak=rules.visible_streak(st.streak, st.last_active_date, today) if st else 0,
                current_lesson=cur.title if cur else None,
                accuracy=accuracy([a for a in data.attempts if a.student_id == student.id]),
                idle_days=idle_days,
                idle=idle,
                behind=idle or bool(overdue),
                overdue=len(overdue),
            )
        )
    rows.sort(key=lambda r: (not r.behind, r.name.lower()))

    names = twin.concept_names(db, [c.id for c in data.courses])
    low: dict[tuple[str, str], int] = defaultdict(int)
    for student, _ in data.students:
        t = twin.insights(db, student.id, at, [c.id for c in data.courses], limit=1000)
        for c in t.weakest:
            if c.mastery < twin.LOW_MASTERY:
                low[(c.kind, c.key)] += 1
    weak = sorted(
        (
            WeakTopic(
                kind=kind,
                key=key,
                name=names.get((kind, key), {"en": key, "hi": key, "kn": key}),
                accuracy=round(sum(r) / len(r), 3),
                attempts=len(r),
                low_mastery_students=low.get((kind, key), 0),
            )
            for (kind, key), r in _concept_accuracy(data).items()
        ),
        key=lambda w: (w.accuracy, -w.attempts),
    )[:WEAK_TOPICS]

    return DashboardOut(
        classroom_id=str(classroom.id),
        name=classroom.name,
        join_key=classroom.join_key,
        school_consent=classroom.school_consent,
        courses=[CourseRef(id=str(c.id), title=c.title) for c in data.courses],
        tiles=Tiles(
            active_this_week=len(active),
            student_count=len(data.students),
            accuracy=round(a, 3) if (a := accuracy(recent)) is not None else None,
            behind=sum(1 for r in rows if r.behind),
        ),
        weak_topics=weak,
        students=rows,
    )


@router.get("/classrooms/{classroom_id}/dashboard")
def class_dashboard(
    classroom_id: uuid.UUID,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    services: ServicesDep,
    course_id: uuid.UUID | None = None,
) -> DashboardOut:
    classroom = own_classroom(db, classroom_id, user.id)
    key = (str(classroom_id), str(course_id) if course_id else None)
    ttl = services.settings.dashboard_cache_seconds
    hit = _cache.get(key)
    if hit and ttl > 0 and time.monotonic() - hit[0] < ttl:
        return hit[1]
    out = build_dashboard(service_db, classroom, course_id, now())
    _cache[key] = (time.monotonic(), out)
    return out


class LessonResultOut(BaseModel):
    lesson_id: str
    title: dict[str, str]
    completed: bool
    accuracy: float | None


class ConceptOut(BaseModel):
    kind: str
    name: dict[str, str]
    mastery: float


class ConfusionOut(BaseModel):
    expected: dict[str, str]
    chosen: dict[str, str]
    count: int


class StudentDetailOut(BaseModel):
    id: str
    name: str
    streak: int
    last_active: datetime | None
    lessons_completed: int
    lessons: list[LessonResultOut]
    weakest: list[ConceptOut]
    confusions: list[ConfusionOut]
    trend: str
    usual_hour: int | None


def week_bounds(at: datetime, weeks_back: int = 0) -> tuple[datetime, datetime]:
    """Monday 00:00 to the next Monday 00:00, India time."""
    local = at.astimezone(rules.INDIA)
    monday = (local - timedelta(days=local.weekday())).replace(
        hour=0, minute=0, second=0, microsecond=0
    )
    start = monday - timedelta(weeks=weeks_back)
    return start, start + timedelta(weeks=1)


def weekly_accuracy(
    attempts: list[AnswerAttempt], at: datetime
) -> tuple[float | None, float | None]:
    this_start, this_end = week_bounds(at)
    last_start, last_end = week_bounds(at, 1)
    this = accuracy([a for a in attempts if this_start <= a.created_at < this_end])
    last = accuracy([a for a in attempts if last_start <= a.created_at < last_end])
    return this, last


@router.get("/classrooms/{classroom_id}/students/{student_id}")
def student_detail(
    classroom_id: uuid.UUID, student_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb
) -> StudentDetailOut:
    """A student's progress for their teacher. Never their doubt chat."""
    classroom = own_classroom(db, classroom_id, user.id)
    data = load_class(service_db, classroom, None)
    found = next(((u, j) for u, j in data.students if u.id == student_id), None)
    if found is None:
        raise api_error(ErrorCode.NOT_FOUND)
    student, joined = found
    at = now()
    mine = [a for a in data.attempts if a.student_id == student.id]
    lessons = []
    for lesson in data.lessons:
        p = data.progress.get((student.id, lesson.id))
        lessons.append(
            LessonResultOut(
                lesson_id=str(lesson.id),
                title=lesson.title,
                completed=p is not None and p.status == "completed",
                accuracy=accuracy([a for a in mine if a.lesson_id == lesson.id]),
            )
        )
    insight = twin.insights(service_db, student.id, at, [c.id for c in data.courses])
    st = data.stats.get(student.id)
    this, last = weekly_accuracy(mine, at)
    return StudentDetailOut(
        id=str(student.id),
        name=student.name,
        streak=rules.visible_streak(st.streak, st.last_active_date, rules.india_today(at))
        if st
        else 0,
        last_active=last_active(data, student, joined) if mine else None,
        lessons_completed=sum(1 for ls in lessons if ls.completed),
        lessons=lessons,
        weakest=[ConceptOut(kind=c.kind, name=c.name, mastery=c.mastery) for c in insight.weakest],
        confusions=[
            ConfusionOut(expected=c.expected, chosen=c.chosen, count=c.count)
            for c in insight.confusions
        ],
        trend=twin_model.trend_word(this, last),
        usual_hour=insight.habits.usual_hour,
    )


class SchoolConsentIn(BaseModel):
    enabled: bool


class SchoolConsentOut(BaseModel):
    classroom_id: str
    school_consent: bool


@router.put("/classrooms/{classroom_id}/school-consent")
def set_school_consent(
    classroom_id: uuid.UUID, body: SchoolConsentIn, user: Me, db: UserDb
) -> SchoolConsentOut:
    """The teacher confirms the school has consent forms on file, so students in this class
    don't need a parent's approval in the app (PRD M4)."""
    classroom = own_classroom(db, classroom_id, user.id)
    classroom.school_consent = body.enabled
    classroom.school_consent_by = uuid.UUID(user.id) if body.enabled else None
    classroom.school_consent_at = now() if body.enabled else None
    db.flush()
    for k in [k for k in _cache if k[0] == str(classroom_id)]:
        _cache.pop(k, None)
    return SchoolConsentOut(classroom_id=str(classroom.id), school_consent=classroom.school_consent)
