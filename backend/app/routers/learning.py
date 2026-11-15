"""Student learning endpoints (PRD M3): home, lesson path, playing lessons (answers graded on
the server, XP, hearts, streaks), term cards, doubt chat and Listen audio."""

import time
import uuid
from collections import defaultdict, deque
from datetime import UTC, date, datetime
from typing import Annotated, Any, Literal

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import func, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.ai import pipeline
from app.ai.doubts import answer_doubt, notes_context
from app.ai.llm import LLMError
from app.ai.pipeline import NeedsReviewError
from app.ai.providers import ServicesDep
from app.auth import CurrentUser, get_current_user
from app.db.models import (
    AnswerAttempt,
    Classroom,
    Course,
    CourseAssignment,
    Doubt,
    Lesson,
    LessonProgress,
    Membership,
    QuestionRow,
    SavedTerm,
    StudentStats,
    TermRow,
    TwinConcept,
    Unit,
    User,
)
from app.db.session import ServiceDb, UserDb
from app.errors import ErrorCode, api_error
from app.learning import rules
from app.learning.audio import audio_url
from app.routers.courses import LessonOut, _question_out, _readable_course
from app.speech.sarvam import SpeechError
from app.twin import model as twin_model
from app.twin import service as twin

router = APIRouter(tags=["learning"])
Me = Annotated[CurrentUser, Depends(get_current_user)]


def now() -> datetime:
    """The current time; tests replace it to move between days."""
    return datetime.now(UTC)


# ---- stats ---------------------------------------------------------------------------------


class StatsOut(BaseModel):
    xp_total: int
    xp_today: int
    daily_goal: int
    hearts: int
    max_hearts: int = rules.MAX_HEARTS
    refill_at: datetime | None
    streak: int
    longest_streak: int


def _load_stats(db: Session, user_id: str, at: datetime) -> StudentStats:
    """The student's stats row (created on first use), with hearts refilled and today's XP
    reset when a new India-time day has started."""
    sid = uuid.UUID(user_id)
    row = db.get(StudentStats, sid)
    if row is None:
        db.execute(insert(StudentStats).values(student_id=sid).on_conflict_do_nothing())
        db.flush()
        row = db.get(StudentStats, sid)
        assert row is not None  # noqa: S101
        row.hearts_updated_at = at
    hearts = rules.refill(row.hearts, row.hearts_updated_at, at)
    row.hearts, row.hearts_updated_at = hearts.count, hearts.updated_at
    today = rules.india_today(at)
    if row.xp_today_date != today:
        row.xp_today, row.xp_today_date = 0, today
    return row


def _stats_out(row: StudentStats, at: datetime) -> StatsOut:
    hearts = rules.Hearts(row.hearts, row.hearts_updated_at)
    today = rules.india_today(at)
    return StatsOut(
        xp_total=row.xp_total,
        xp_today=rules.xp_today(row.xp_today, row.xp_today_date, today),
        daily_goal=row.daily_goal,
        hearts=row.hearts,
        refill_at=hearts.refill_at,
        streak=rules.visible_streak(row.streak, row.last_active_date, today),
        longest_streak=row.longest_streak,
    )


@router.get("/me/stats")
def my_stats(user: Me, db: UserDb) -> StatsOut:
    at = now()
    return _stats_out(_load_stats(db, user.id, at), at)


class DailyGoalIn(BaseModel):
    goal: Literal[30, 50, 100]


@router.put("/me/daily-goal")
def set_daily_goal(body: DailyGoalIn, user: Me, db: UserDb) -> StatsOut:
    at = now()
    row = _load_stats(db, user.id, at)
    row.daily_goal = body.goal
    db.flush()
    return _stats_out(row, at)


# ---- lesson path ---------------------------------------------------------------------------


class PathLesson(BaseModel):
    id: str
    position: int
    title: dict[str, str]
    state: Literal["done", "current", "locked"]
    generation_status: str
    question_count: int
    unlocked_by: dict[str, str] | None = None


class PathUnit(BaseModel):
    id: str
    title: dict[str, str]
    lessons: list[PathLesson]


class PathOut(BaseModel):
    course_id: str
    title: dict[str, str]
    subject: str
    is_personal: bool
    units: list[PathUnit]
    done_count: int
    lesson_count: int
    current_lesson_id: str | None


def _course_lessons(db: Session, course_id: uuid.UUID) -> list[tuple[Unit, list[Lesson]]]:
    """Units and their lessons in order, as this user may see them (RLS hides unpublished
    lessons of class courses from students)."""
    units = db.scalars(
        select(Unit).where(Unit.course_id == course_id).order_by(Unit.position)
    ).all()
    lessons = db.scalars(
        select(Lesson).where(Lesson.course_id == course_id).order_by(Lesson.position)
    ).all()
    by_unit: dict[uuid.UUID, list[Lesson]] = defaultdict(list)
    for lesson in lessons:
        by_unit[lesson.unit_id].append(lesson)
    return [(u, by_unit[u.id]) for u in units if by_unit[u.id]]


def _completed(db: Session, user_id: str, lesson_ids: list[uuid.UUID]) -> set[uuid.UUID]:
    if not lesson_ids:
        return set()
    return set(
        db.scalars(
            select(LessonProgress.lesson_id).where(
                LessonProgress.student_id == uuid.UUID(user_id),
                LessonProgress.lesson_id.in_(lesson_ids),
                LessonProgress.status == "completed",
            )
        )
    )


def _build_path(db: Session, course: Course, user_id: str) -> PathOut:
    grouped = _course_lessons(db, course.id)
    ordered = [lesson for _, lessons in grouped for lesson in lessons]
    done = _completed(db, user_id, [lesson.id for lesson in ordered])
    counts = dict(
        db.execute(
            select(QuestionRow.lesson_id, func.count())
            .where(QuestionRow.lesson_id.in_([lesson.id for lesson in ordered]))
            .group_by(QuestionRow.lesson_id)
        ).all()
    )
    current: Lesson | None = next((ls for ls in ordered if ls.id not in done), None)
    units: list[PathUnit] = []
    previous: Lesson | None = None
    for unit, lessons in grouped:
        out: list[PathLesson] = []
        for lesson in lessons:
            if lesson.id in done:
                state: Literal["done", "current", "locked"] = "done"
            elif current is not None and lesson.id == current.id:
                state = "current"
            else:
                state = "locked"
            out.append(
                PathLesson(
                    id=str(lesson.id),
                    position=lesson.position,
                    title=lesson.title,
                    state=state,
                    generation_status=lesson.generation_status,
                    question_count=int(counts.get(lesson.id, 0)),
                    unlocked_by=previous.title if state == "locked" and previous else None,
                )
            )
            previous = lesson
        units.append(PathUnit(id=str(unit.id), title=unit.title, lessons=out))
    return PathOut(
        course_id=str(course.id),
        title=course.title,
        subject=course.subject,
        is_personal=course.is_personal,
        units=units,
        done_count=len(done),
        lesson_count=len(ordered),
        current_lesson_id=str(current.id) if current else None,
    )


@router.get("/courses/{course_id}/path")
def course_path(course_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb) -> PathOut:
    course, _ = _readable_course(db, service_db, course_id, user.id)
    return _build_path(db, course, user.id)


# ---- home ----------------------------------------------------------------------------------


class ContinueOut(BaseModel):
    course_id: str
    course_title: dict[str, str]
    subject: str
    lesson_id: str
    lesson_title: dict[str, str]
    lesson_number: int
    lesson_count: int
    generation_status: str
    due_at: datetime | None = None


class HomeClass(BaseModel):
    id: str
    name: str
    subject: str
    teacher_name: str
    new_lessons: int


class HomeCourse(BaseModel):
    id: str
    title: dict[str, str]
    subject: str
    is_personal: bool
    ai_made: bool
    percent_complete: int
    lesson_count: int


class HomeOut(BaseModel):
    name: str
    language_mode: str
    stats: StatsOut
    continue_lesson: ContinueOut | None
    classes: list[HomeClass]
    courses: list[HomeCourse]


def _student_courses(db: Session, user_id: str) -> list[tuple[Course, datetime | None, bool]]:
    """(course, due date, is class course) in Continue order: class courses with a due date
    (earliest first), other class courses, then personal courses."""
    me = uuid.UUID(user_id)
    assigned = db.execute(
        select(Course, func.min(CourseAssignment.due_at))
        .join(CourseAssignment, CourseAssignment.course_id == Course.id)
        .join(Membership, Membership.classroom_id == CourseAssignment.classroom_id)
        .where(Membership.student_id == me, Membership.status == "active")
        .group_by(Course.id)
    ).all()
    personal = db.scalars(
        select(Course)
        .where(Course.owner_id == me, Course.is_personal.is_(True))
        .order_by(Course.created_at)
    ).all()
    far = datetime.max.replace(tzinfo=UTC)
    class_courses = sorted(assigned, key=lambda r: (r[1] is None, r[1] or far, r[0].created_at))
    return [(c, due, True) for c, due in class_courses] + [(c, None, False) for c in personal]


@router.get("/me/home")
def home(user: Me, db: UserDb) -> HomeOut:
    at = now()
    me = db.get(User, uuid.UUID(user.id))
    if me is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    stats = _load_stats(db, user.id, at)

    continue_lesson: ContinueOut | None = None
    courses: list[HomeCourse] = []
    new_by_course: dict[uuid.UUID, int] = {}
    for course, due, is_class in _student_courses(db, user.id):
        path = _build_path(db, course, user.id)
        if path.lesson_count == 0:
            continue
        lessons = [ls for unit in path.units for ls in unit.lessons]
        new_by_course[course.id] = sum(1 for ls in lessons if ls.state != "done")
        courses.append(
            HomeCourse(
                id=str(course.id),
                title=course.title,
                subject=course.subject,
                is_personal=course.is_personal,
                ai_made=course.is_personal,
                percent_complete=round(100 * path.done_count / path.lesson_count),
                lesson_count=path.lesson_count,
            )
        )
        if continue_lesson is None and path.current_lesson_id is not None:
            number, current = next(
                (i + 1, ls) for i, ls in enumerate(lessons) if ls.id == path.current_lesson_id
            )
            continue_lesson = ContinueOut(
                course_id=str(course.id),
                course_title=course.title,
                subject=course.subject,
                lesson_id=current.id,
                lesson_title=current.title,
                lesson_number=number,
                lesson_count=path.lesson_count,
                generation_status=current.generation_status,
                due_at=due if is_class else None,
            )

    class_rows = db.execute(
        select(Classroom, User.name)
        .join(Membership, Membership.classroom_id == Classroom.id)
        .join(User, User.id == Classroom.teacher_id)
        .where(Membership.student_id == uuid.UUID(user.id), Membership.status == "active")
        .order_by(Membership.joined_at.desc())
    ).all()
    classes = []
    for c, teacher_name in class_rows:
        course_ids = db.scalars(
            select(CourseAssignment.course_id).where(CourseAssignment.classroom_id == c.id)
        ).all()
        classes.append(
            HomeClass(
                id=str(c.id),
                name=c.name,
                subject=c.subject,
                teacher_name=teacher_name,
                new_lessons=sum(new_by_course.get(cid, 0) for cid in course_ids),
            )
        )
    return HomeOut(
        name=me.name,
        language_mode=me.language_mode,
        stats=_stats_out(stats, at),
        continue_lesson=continue_lesson,
        classes=classes,
        courses=courses,
    )


# ---- playing a lesson ----------------------------------------------------------------------


def require_consent(db: Session, service_db: Session, user_id: str, course: Course) -> None:
    """DPDP (M4): students need a parent's approval, or a class whose school has collected
    consent, before starting lessons. Course owners (teachers previewing) aren't students here."""
    me = db.get(User, uuid.UUID(user_id))
    if (
        me is None
        or "student" not in me.roles
        or str(course.owner_id) == user_id
        and not course.is_personal
    ):
        return
    if me.consent_status == "approved":
        return
    if me.consent_status != "revoked":
        school = service_db.scalar(
            select(func.count())
            .select_from(Membership)
            .join(Classroom, Classroom.id == Membership.classroom_id)
            .where(
                Membership.student_id == me.id,
                Membership.status == "active",
                Classroom.school_consent.is_(True),
            )
        )
        if school:
            return
    raise api_error(ErrorCode.CONSENT_REQUIRED)


class StartOut(BaseModel):
    lesson: LessonOut
    stats: StatsOut
    # The Student Twin's mastery of each term (0..1) for "You know this: N%" on term cards.
    term_mastery: dict[str, float] = {}


def _term_mastery(
    db: Session, student_id: uuid.UUID, course_id: uuid.UUID, at: datetime
) -> dict[str, float]:
    rows = db.scalars(
        select(TwinConcept).where(
            TwinConcept.student_id == student_id,
            TwinConcept.course_id == course_id,
            TwinConcept.kind == "term",
        )
    )
    return {
        r.key: round(twin_model.decayed(r.mastery, r.last_seen_at, r.half_life_days, at), 3)
        for r in rows
    }


def _playable_lesson(
    db: Session, service_db: Session, lesson_id: uuid.UUID, user_id: str
) -> tuple[Lesson, Course]:
    lesson = service_db.get(Lesson, lesson_id)
    if lesson is None:
        raise api_error(ErrorCode.NOT_FOUND)
    course, is_owner = _readable_course(db, service_db, lesson.course_id, user_id)
    if not is_owner and lesson.published_at is None:
        raise api_error(ErrorCode.LESSON_NOT_PUBLISHED)
    return lesson, course


@router.post("/lessons/{lesson_id}/start")
def start_lesson(
    lesson_id: uuid.UUID,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    services: ServicesDep,
    background: BackgroundTasks,
) -> StartOut:
    lesson, course = _playable_lesson(db, service_db, lesson_id, user.id)
    require_consent(db, service_db, user.id, course)
    if lesson.generation_status != "ready":
        if lesson.generation_status == "not_started" and course.is_personal:
            # A personal course's next lesson is made when the student reaches it.
            started = pipeline.claim_lesson(services, lesson.id)
            if started:
                background.add_task(
                    services.jobs.enqueue, "generate_lesson", lesson_id=str(lesson.id)
                )
        raise api_error(ErrorCode.LESSON_NOT_READY)
    # Starting lesson N makes lesson N+1 (lazy generation, exactly once).
    following = pipeline.claim_next_lesson(services, lesson.id)
    if following is not None:
        background.add_task(services.jobs.enqueue, "generate_lesson", lesson_id=str(following))

    at = now()
    db.execute(
        insert(LessonProgress)
        .values(student_id=uuid.UUID(user.id), lesson_id=lesson.id, started_at=at)
        .on_conflict_do_update(
            index_elements=[LessonProgress.student_id, LessonProgress.lesson_id],
            set_={"started_at": at},
        )
    )
    questions = db.scalars(
        select(QuestionRow).where(QuestionRow.lesson_id == lesson.id).order_by(QuestionRow.position)
    ).all()
    terms = {
        t.key: t.content for t in db.scalars(select(TermRow).where(TermRow.course_id == course.id))
    }
    out = LessonOut(
        id=str(lesson.id),
        course_id=str(course.id),
        title=lesson.title,
        generation_status=lesson.generation_status,
        published=lesson.published_at is not None,
        questions=[_question_out(q, owner=False) for q in questions],
        terms=terms,
    )
    stats = _load_stats(db, user.id, at)
    return StartOut(
        lesson=out,
        stats=_stats_out(stats, at),
        term_mastery=_term_mastery(db, uuid.UUID(user.id), course.id, at),
    )


class AttemptIn(BaseModel):
    question_id: uuid.UUID
    answer: dict[str, Any]
    retry: bool = False
    time_ms: int = Field(default=0, ge=0, le=3_600_000)


class AttemptOut(BaseModel):
    correct: bool
    correct_answer: dict[str, Any]
    explanation: dict[str, str]
    xp_delta: int
    heart_lost: bool
    stats: StatsOut


def _hearts_empty(refill_at: datetime | None) -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_409_CONFLICT,
        detail={
            "code": ErrorCode.HEARTS_EMPTY.value,
            "refill_at": refill_at.isoformat() if refill_at else None,
        },
    )


@router.post("/attempts")
def submit_attempt(body: AttemptIn, user: Me, db: UserDb, service_db: ServiceDb) -> AttemptOut:
    question = db.get(QuestionRow, body.question_id)  # RLS: only lessons this user may read
    if question is None:
        raise api_error(ErrorCode.NOT_FOUND)
    at = now()
    stats = _load_stats(db, user.id, at)
    try:
        result = rules.grade(question.type, question.content, body.answer)
    except rules.InvalidAnswerError as exc:
        raise api_error(ErrorCode.INVALID_ANSWER) from exc
    if stats.hearts <= 0 and result.costs_heart:
        raise _hearts_empty(rules.Hearts(stats.hearts, stats.hearts_updated_at).refill_at)

    xp = rules.answer_xp(result.correct, body.retry)
    heart_lost = not result.correct and result.costs_heart
    if heart_lost:
        hearts = rules.lose_heart(rules.Hearts(stats.hearts, stats.hearts_updated_at), at)
        stats.hearts, stats.hearts_updated_at = hearts.count, hearts.updated_at
    stats.xp_total += xp
    stats.xp_today += xp
    db.add(
        AnswerAttempt(
            student_id=uuid.UUID(user.id),
            question_id=question.id,
            lesson_id=question.lesson_id,
            correct=result.correct,
            first_try=not body.retry,
            answer=body.answer,
            time_ms=body.time_ms,
        )
    )
    db.flush()
    lesson = db.get(Lesson, question.lesson_id)
    if lesson is not None:
        # The Student Twin learns from every answer (written by the backend only).
        twin.record_attempt(
            service_db,
            student_id=uuid.UUID(user.id),
            course_id=lesson.course_id,
            question=question,
            answer=body.answer,
            correct=result.correct,
            retry=body.retry,
            at=at,
        )
    return AttemptOut(
        correct=result.correct,
        correct_answer=result.correct_answer,
        explanation=question.content.get("explanation") or {},
        xp_delta=xp,
        heart_lost=heart_lost,
        stats=_stats_out(stats, at),
    )


class CompleteOut(BaseModel):
    xp_earned: int
    accuracy: float
    streak: int
    streak_extended: bool
    stats: StatsOut


@router.post("/lessons/{lesson_id}/complete")
def complete_lesson(
    lesson_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb
) -> CompleteOut:
    lesson, _ = _playable_lesson(db, service_db, lesson_id, user.id)
    sid = uuid.UUID(user.id)
    progress = db.get(LessonProgress, (sid, lesson.id))
    if progress is None:
        raise api_error(ErrorCode.LESSON_NOT_STARTED)
    attempts = db.scalars(
        select(AnswerAttempt).where(
            AnswerAttempt.student_id == sid,
            AnswerAttempt.lesson_id == lesson.id,
            AnswerAttempt.created_at >= progress.started_at,
        )
    ).all()
    first = [a for a in attempts if a.first_try]
    accuracy = sum(a.correct for a in first) / len(first) if first else 0.0
    answer_xp = sum(rules.answer_xp(a.correct, not a.first_try) for a in attempts)

    at = now()
    stats = _load_stats(db, user.id, at)
    streak = rules.finish_lesson_streak(
        stats.streak, stats.longest_streak, stats.last_active_date, rules.india_today(at)
    )
    stats.streak, stats.longest_streak = streak.current, streak.longest
    stats.last_active_date = streak.last_active
    stats.xp_total += rules.XP_LESSON_BONUS
    stats.xp_today += rules.XP_LESSON_BONUS
    progress.status = "completed"
    progress.completed_at = at
    progress.best_accuracy = max(progress.best_accuracy, accuracy)
    progress.xp_earned = answer_xp + rules.XP_LESSON_BONUS
    db.flush()
    return CompleteOut(
        xp_earned=answer_xp + rules.XP_LESSON_BONUS,
        accuracy=round(accuracy, 3),
        streak=streak.current,
        streak_extended=streak.extended,
        stats=_stats_out(stats, at),
    )


# ---- terms ---------------------------------------------------------------------------------


class TermOut(BaseModel):
    key: str
    content: dict[str, Any]
    source_page: int
    saved: bool
    mastery: float | None = None


@router.get("/courses/{course_id}/terms")
def course_terms(
    course_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb
) -> list[TermOut]:
    course, _ = _readable_course(db, service_db, course_id, user.id)
    saved = set(
        db.scalars(
            select(SavedTerm.term_key).where(
                SavedTerm.student_id == uuid.UUID(user.id), SavedTerm.course_id == course.id
            )
        )
    )
    rows = db.scalars(select(TermRow).where(TermRow.course_id == course.id).order_by(TermRow.key))
    mastery = _term_mastery(db, uuid.UUID(user.id), course.id, now())
    return [
        TermOut(
            key=t.key,
            content=t.content,
            source_page=t.source_page,
            saved=t.key in saved,
            mastery=mastery.get(t.key),
        )
        for t in rows
    ]


@router.put("/me/saved-terms/{course_id}/{key}", status_code=status.HTTP_204_NO_CONTENT)
def save_term(course_id: uuid.UUID, key: str, user: Me, db: UserDb, service_db: ServiceDb) -> None:
    course, _ = _readable_course(db, service_db, course_id, user.id)
    if db.scalar(select(TermRow).where(TermRow.course_id == course.id, TermRow.key == key)) is None:
        raise api_error(ErrorCode.NOT_FOUND)
    db.execute(
        insert(SavedTerm)
        .values(student_id=uuid.UUID(user.id), course_id=course.id, term_key=key)
        .on_conflict_do_nothing()
    )


@router.delete("/me/saved-terms/{course_id}/{key}", status_code=status.HTTP_204_NO_CONTENT)
def unsave_term(course_id: uuid.UUID, key: str, user: Me, db: UserDb) -> None:
    row = db.get(SavedTerm, (uuid.UUID(user.id), course_id, key))
    if row is not None:
        db.delete(row)


# ---- doubts --------------------------------------------------------------------------------


class DoubtIn(BaseModel):
    course_id: uuid.UUID
    question_id: uuid.UUID | None = None
    term_key: str | None = Field(default=None, max_length=60)
    text: str = Field(min_length=1, max_length=500)
    mode: Literal["ask", "simpler", "example"] = "ask"


class DoubtOut(BaseModel):
    id: str
    answer: dict[str, str]
    in_notes: bool
    source_pages: list[int]
    doubts_left_today: int


def _india_day_start(at: datetime) -> datetime:
    day: date = rules.india_today(at)
    return datetime(day.year, day.month, day.day, tzinfo=rules.INDIA)


@router.post("/doubts")
def ask_doubt(
    body: DoubtIn, user: Me, db: UserDb, service_db: ServiceDb, services: ServicesDep
) -> DoubtOut:
    sid = uuid.UUID(user.id)
    at = now()
    limit = services.settings.doubts_per_day
    asked = (
        db.scalar(
            select(func.count())
            .select_from(Doubt)
            .where(Doubt.student_id == sid, Doubt.created_at >= _india_day_start(at))
        )
        or 0
    )
    if asked >= limit:
        raise api_error(ErrorCode.DOUBT_LIMIT)
    course, _ = _readable_course(db, service_db, body.course_id, user.id)
    me = db.get(User, sid)
    about_parts: list[str] = []
    if body.question_id is not None:
        question = db.get(QuestionRow, body.question_id)
        if question is None:
            raise api_error(ErrorCode.NOT_FOUND)
        prompt = question.content.get("prompt") or {}
        about_parts.append(f'the practice question "{prompt.get("en", "")}"')
        explanation = question.content.get("explanation") or {}
        if explanation.get("en"):
            about_parts.append(f"(its explanation: {explanation['en']})")
    if body.term_key:
        term = db.scalar(
            select(TermRow).where(TermRow.course_id == course.id, TermRow.key == body.term_key)
        )
        if term is not None:
            about_parts.append(
                f'the term "{(term.content.get("forms") or {}).get("en", term.key)}"'
            )
    # Note chunks are readable only by the course owner under RLS; the student's access to
    # the course was checked above, so read them with the service session.
    notes = notes_context(services, service_db, course.upload_id, body.text)
    learner = twin.doubt_context(service_db, sid, course.id, at)
    try:
        result = answer_doubt(
            services,
            notes=notes,
            doubt=body.text,
            about=" ".join(about_parts),
            language_mode=me.language_mode if me else "hindi",
            grade=course.grade,
            mode=body.mode,
            learner=learner,
        )
    except (NeedsReviewError, LLMError) as exc:
        raise api_error(ErrorCode.AI_UNAVAILABLE) from exc
    row = Doubt(
        student_id=sid,
        course_id=course.id,
        question_id=body.question_id,
        term_key=body.term_key,
        mode=body.mode,
        text=body.text,
        answer=result.answer,
        source_pages=result.source_pages,
        in_notes=result.in_notes,
        prompt_version=result.prompt_version,
    )
    db.add(row)
    db.flush()
    return DoubtOut(
        id=str(row.id),
        answer=result.answer,
        in_notes=result.in_notes,
        source_pages=result.source_pages,
        doubts_left_today=max(0, limit - asked - 1),
    )


# ---- audio ---------------------------------------------------------------------------------


class AudioIn(BaseModel):
    text: str = Field(min_length=1, max_length=2500)
    language: Literal["hi", "kn", "en"]


class AudioOut(BaseModel):
    url: str


_audio_calls: dict[str, deque[float]] = defaultdict(deque)


def _audio_rate_limit(user_id: str, per_hour: int) -> None:
    calls = _audio_calls[user_id]
    cutoff = time.monotonic() - 3600
    while calls and calls[0] < cutoff:
        calls.popleft()
    if len(calls) >= per_hour:
        raise api_error(ErrorCode.TOO_MANY_ATTEMPTS)
    calls.append(time.monotonic())


@router.post("/audio")
def get_audio(body: AudioIn, user: Me, services: ServicesDep) -> AudioOut:
    _audio_rate_limit(user.id, services.settings.audio_requests_per_hour)
    if len(body.text) > services.settings.tts_max_chars:
        raise api_error(ErrorCode.INVALID_ANSWER)
    try:
        return AudioOut(url=audio_url(services, body.text, body.language))
    except SpeechError as exc:
        raise api_error(ErrorCode.AI_UNAVAILABLE) from exc
