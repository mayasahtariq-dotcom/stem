"""Parent mode (PRD M4): students create a link code; parents link, approve consent (DPDP),
revoke it, request deletion, and see a read-only dashboard for each child.

Parents never see doubts, other students, class rank or the leaderboard, and can't edit
anything about the child's learning. Every parent request first checks an active link through
the parent's own session (RLS on parent_links), then reads the child's data.
"""

import logging
import secrets
import uuid
from datetime import UTC, datetime, timedelta
from typing import Annotated, Literal

from fastapi import APIRouter, BackgroundTasks, Depends, status
from pydantic import BaseModel, Field
from sqlalchemy import delete, func, select, text, update
from sqlalchemy.orm import Session

from app.ai.providers import Services, ServicesDep
from app.auth import CurrentUser, get_current_user
from app.classrooms.keys import ALPHABET
from app.db.models import (
    AnswerAttempt,
    Classroom,
    ConsentRecord,
    Course,
    CourseAssignment,
    DeletionRequest,
    Lesson,
    LessonProgress,
    LinkCode,
    Membership,
    ParentLink,
    StudentStats,
    TwinConcept,
    User,
)
from app.db.session import ServiceDb, UserDb
from app.errors import ErrorCode, api_error
from app.learning import rules
from app.routers.dashboard import accuracy, week_bounds
from app.twin import model as twin_model
from app.twin import service as twin

router = APIRouter(tags=["parents"])
Me = Annotated[CurrentUser, Depends(get_current_user)]
log = logging.getLogger(__name__)

CODE_LENGTH = 6
CODE_TTL = timedelta(hours=24)
MAX_PARENTS = 2


def now() -> datetime:
    """The current time; tests replace it."""
    return datetime.now(UTC)


def _user(db: Session, user_id: str) -> User:
    me = db.get(User, uuid.UUID(user_id))
    if me is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    return me


# ---- student side --------------------------------------------------------------------------


class LinkCodeOut(BaseModel):
    code: str
    expires_at: datetime


@router.post("/me/link-code", status_code=status.HTTP_201_CREATED)
def create_link_code(user: Me, db: UserDb, service_db: ServiceDb) -> LinkCodeOut:
    """A six-character code (and QR) a parent uses to link. Lasts 24 hours, works once."""
    me = _user(db, user.id)
    if "student" not in me.roles:
        raise api_error(ErrorCode.NOT_A_STUDENT)
    at = now()
    for _ in range(20):
        code = "".join(secrets.choice(ALPHABET) for _ in range(CODE_LENGTH))
        if service_db.get(LinkCode, code) is None:
            break
    else:  # pragma: no cover - 31^6 codes
        raise api_error(ErrorCode.KEY_GENERATION_FAILED)
    service_db.add(LinkCode(code=code, student_id=me.id, expires_at=at + CODE_TTL))
    return LinkCodeOut(code=code, expires_at=at + CODE_TTL)


class ParentOut(BaseModel):
    id: str
    name: str
    phone_last4: str
    linked_at: datetime
    consented: bool


@router.get("/me/parents")
def my_parents(user: Me, db: UserDb) -> list[ParentOut]:
    rows = db.execute(
        select(ParentLink, User)
        .join(User, User.id == ParentLink.parent_id)
        .where(ParentLink.student_id == uuid.UUID(user.id), ParentLink.status == "active")
        .order_by(ParentLink.linked_at)
    ).all()
    return [
        ParentOut(
            id=str(p.id),
            name=p.name,
            phone_last4=p.phone[-4:],
            linked_at=link.linked_at,
            consented=link.consent_at is not None,
        )
        for link, p in rows
    ]


def _refresh_consent(service_db: Session, student_id: uuid.UUID) -> None:
    """A student stays approved while at least one active parent has consented."""
    student = service_db.get(User, student_id)
    if student is None or student.consent_status == "revoked":
        return
    consenting = service_db.scalar(
        select(func.count())
        .select_from(ParentLink)
        .where(
            ParentLink.student_id == student_id,
            ParentLink.status == "active",
            ParentLink.consent_at.is_not(None),
        )
    )
    student.consent_status = "approved" if consenting else "pending"


@router.delete("/me/parents/{parent_id}", status_code=status.HTTP_204_NO_CONTENT)
def remove_parent(parent_id: uuid.UUID, user: Me, service_db: ServiceDb) -> None:
    _end_link(service_db, parent_id, uuid.UUID(user.id))


def _end_link(service_db: Session, parent_id: uuid.UUID, student_id: uuid.UUID) -> None:
    link = service_db.scalar(
        select(ParentLink).where(
            ParentLink.parent_id == parent_id,
            ParentLink.student_id == student_id,
            ParentLink.status == "active",
        )
    )
    if link is None:
        raise api_error(ErrorCode.NOT_FOUND)
    link.status = "revoked"
    link.revoked_at = now()
    service_db.flush()
    _refresh_consent(service_db, student_id)


# ---- parent side ---------------------------------------------------------------------------


def _parent(db: Session, user_id: str) -> User:
    me = _user(db, user_id)
    if "parent" not in me.roles:
        raise api_error(ErrorCode.NOT_A_PARENT)
    return me


class ClassOut(BaseModel):
    id: str
    name: str
    teacher_name: str


class ChildPreview(BaseModel):
    id: str
    name: str
    classes: list[ClassOut]


def _classes(service_db: Session, student_id: uuid.UUID) -> list[ClassOut]:
    teacher = User.__table__.alias("teacher")
    rows = service_db.execute(
        select(Classroom.id, Classroom.name, teacher.c.name)
        .join(Membership, Membership.classroom_id == Classroom.id)
        .join(teacher, teacher.c.id == Classroom.teacher_id)
        .where(Membership.student_id == student_id, Membership.status == "active")
        .order_by(Classroom.name)
    ).all()
    return [ClassOut(id=str(cid), name=name, teacher_name=tname) for cid, name, tname in rows]


def _valid_code(service_db: Session, raw: str) -> LinkCode:
    code = service_db.get(LinkCode, raw.strip().upper())
    if code is None:
        raise api_error(ErrorCode.CODE_NOT_FOUND)
    if code.used_at is not None or code.expires_at <= now():
        raise api_error(ErrorCode.CODE_EXPIRED)
    return code


@router.get("/parent/link/{code}")
def preview_link(code: str, user: Me, db: UserDb, service_db: ServiceDb) -> ChildPreview:
    _parent(db, user.id)
    lc = _valid_code(service_db, code)
    child = service_db.get(User, lc.student_id)
    if child is None:
        raise api_error(ErrorCode.CODE_NOT_FOUND)
    return ChildPreview(id=str(child.id), name=child.name, classes=_classes(service_db, child.id))


class LinkIn(BaseModel):
    code: str = Field(min_length=CODE_LENGTH, max_length=CODE_LENGTH + 4)


class ChildOut(BaseModel):
    id: str
    name: str
    consent_status: str
    consent_given_by_me: bool
    classes: list[ClassOut]


def _child_out(service_db: Session, child: User, link: ParentLink) -> ChildOut:
    return ChildOut(
        id=str(child.id),
        name=child.name,
        consent_status=child.consent_status,
        consent_given_by_me=link.consent_at is not None,
        classes=_classes(service_db, child.id),
    )


@router.post("/parent/link", status_code=status.HTTP_201_CREATED)
def link_child(body: LinkIn, user: Me, db: UserDb, service_db: ServiceDb) -> ChildOut:
    parent = _parent(db, user.id)
    lc = _valid_code(service_db, body.code)
    child = service_db.get(User, lc.student_id)
    if child is None:
        raise api_error(ErrorCode.CODE_NOT_FOUND)
    active = service_db.scalar(
        select(func.count())
        .select_from(ParentLink)
        .where(
            ParentLink.student_id == child.id,
            ParentLink.status == "active",
            ParentLink.parent_id != parent.id,
        )
    )
    if (active or 0) >= MAX_PARENTS:
        raise api_error(ErrorCode.TOO_MANY_PARENTS)
    at = now()
    link = service_db.scalar(
        select(ParentLink).where(
            ParentLink.parent_id == parent.id, ParentLink.student_id == child.id
        )
    )
    if link is None:
        link = ParentLink(parent_id=parent.id, student_id=child.id, linked_at=at)
        service_db.add(link)
    else:
        link.status, link.linked_at, link.revoked_at, link.consent_at = "active", at, None, None
    lc.used_at, lc.used_by = at, parent.id
    service_db.flush()
    return _child_out(service_db, child, link)


def _linked_child(
    db: Session, service_db: Session, parent_id: str, child_id: uuid.UUID
) -> tuple[User, ParentLink]:
    """The child, if the parent has an active link (checked through the parent's session)."""
    link = db.scalar(
        select(ParentLink).where(
            ParentLink.parent_id == uuid.UUID(parent_id),
            ParentLink.student_id == child_id,
            ParentLink.status == "active",
        )
    )
    child = service_db.get(User, child_id) if link is not None else None
    if link is None or child is None:
        raise api_error(ErrorCode.NOT_FOUND)
    return child, service_db.get(ParentLink, link.id) or link


@router.get("/parent/children")
def my_children(user: Me, db: UserDb, service_db: ServiceDb) -> list[ChildOut]:
    _parent(db, user.id)
    links = db.scalars(
        select(ParentLink)
        .where(ParentLink.parent_id == uuid.UUID(user.id), ParentLink.status == "active")
        .order_by(ParentLink.linked_at)
    ).all()
    out = []
    for link in links:
        child = service_db.get(User, link.student_id)
        if child is not None:
            out.append(_child_out(service_db, child, link))
    return out


@router.post("/parent/children/{child_id}/consent")
def approve_consent(child_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb) -> ChildOut:
    """ "I approve": the parent consents to the child's learning data being processed."""
    child, link = _linked_child(db, service_db, user.id, child_id)
    at = now()
    link.consent_at = at
    service_db.add(
        ConsentRecord(student_id=child.id, source="parent", parent_id=link.parent_id, given_at=at)
    )
    child.consent_status = "approved"
    service_db.flush()
    return _child_out(service_db, child, link)


@router.post("/parent/children/{child_id}/revoke-consent")
def revoke_consent(child_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb) -> ChildOut:
    """Pauses the child's account: no lessons until consent is given again."""
    child, link = _linked_child(db, service_db, user.id, child_id)
    at = now()
    service_db.execute(
        update(ConsentRecord)
        .where(ConsentRecord.student_id == child.id, ConsentRecord.revoked_at.is_(None))
        .values(revoked_at=at)
    )
    service_db.execute(
        update(ParentLink).where(ParentLink.student_id == child.id).values(consent_at=None)
    )
    child.consent_status = "revoked"
    service_db.flush()
    service_db.refresh(link)
    return _child_out(service_db, child, link)


@router.delete("/parent/children/{child_id}", status_code=status.HTTP_204_NO_CONTENT)
def unlink_child(child_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb) -> None:
    _linked_child(db, service_db, user.id, child_id)
    _end_link(service_db, uuid.UUID(user.id), child_id)


class DeletionOut(BaseModel):
    request_id: str
    status: Literal["requested"]
    complete_by: datetime


@router.post("/parent/children/{child_id}/deletion", status_code=status.HTTP_202_ACCEPTED)
def request_deletion(
    child_id: uuid.UUID,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    services: ServicesDep,
    background: BackgroundTasks,
) -> DeletionOut:
    """Deletes the child's account and learning data within seven days (a background job)."""
    child, _ = _linked_child(db, service_db, user.id, child_id)
    student_id = child.id
    service_db.expunge(child)  # the request session must hold no lock on the child's rows
    from app.ai.pipeline import _session

    # Recorded in its own committed transaction (like claiming an upload), so the job that
    # runs after the response can see it and the request never holds a lock the job needs.
    at = now()
    with _session(services) as s, s.begin():
        req = DeletionRequest(
            student_id=student_id, requested_by=uuid.UUID(user.id), requested_at=at
        )
        s.add(req)
        s.execute(update(User).where(User.id == student_id).values(consent_status="revoked"))
        s.flush()
        request_id = str(req.id)
    background.add_task(services.jobs.enqueue, "process_deletion", request_id=request_id)
    return DeletionOut(
        request_id=request_id, status="requested", complete_by=at + timedelta(days=7)
    )


def process_deletion(services: Services, request_id: str) -> None:
    """Deletes the student's profile, which removes their learning data, twin, links and
    consent records with it (foreign keys cascade), then marks the request done."""
    from app.ai.pipeline import _session

    with _session(services) as s, s.begin():
        req = s.get(DeletionRequest, uuid.UUID(request_id))
        if req is None or req.completed_at is not None:
            return
        sid = req.student_id
        s.execute(delete(User).where(User.id == sid))
        if services.settings.local_auth_stub:
            s.execute(text("DELETE FROM auth.users WHERE id = :id"), {"id": sid})
        req.completed_at = datetime.now(UTC)
    # VERIFY: in production also delete the Supabase auth account with the admin API
    # (DELETE /auth/v1/admin/users/{id}, service key). Logged for follow-up.
    log.info("student data deleted", extra={"request_id": request_id})


def register_parent_jobs(services: Services) -> None:
    services.jobs.register(
        "process_deletion", lambda request_id: process_deletion(services, request_id)
    )


# ---- the parent dashboard ------------------------------------------------------------------


class WeekCard(BaseModel):
    lessons_this_week: int
    lessons_last_week: int
    arrow: Literal["up", "down", "same"]
    minutes_this_week: int
    streak: int


class AccuracyCard(BaseModel):
    this_week: float | None
    last_week: float | None
    word: Literal["improving", "steady", "needs_practice"]


class SubjectCard(BaseModel):
    subject: str
    strongest: dict[str, str] | None
    weakest: dict[str, str] | None


class AssignmentOut(BaseModel):
    course_id: str
    title: dict[str, str]
    classroom: str
    due_at: datetime | None
    status: Literal["done", "pending", "overdue"]


class ChildDashboard(BaseModel):
    child: ChildOut
    has_activity: bool
    week: WeekCard
    accuracy: AccuracyCard
    subjects: list[SubjectCard]
    assignments: list[AssignmentOut]
    classes: list[ClassOut]
    summary: dict[str, str]


def _assignments(service_db: Session, child_id: uuid.UUID, at: datetime) -> list[AssignmentOut]:
    rows = service_db.execute(
        select(CourseAssignment, Course, Classroom.name)
        .join(Course, Course.id == CourseAssignment.course_id)
        .join(Classroom, Classroom.id == CourseAssignment.classroom_id)
        .join(Membership, Membership.classroom_id == Classroom.id)
        .where(Membership.student_id == child_id, Membership.status == "active")
        .order_by(CourseAssignment.due_at.nulls_last())
    ).all()
    out = []
    for a, course, cname in rows:
        lessons = service_db.scalars(
            select(Lesson.id).where(Lesson.course_id == course.id, Lesson.published_at.is_not(None))
        ).all()
        done_count = (
            service_db.scalar(
                select(func.count())
                .select_from(LessonProgress)
                .where(
                    LessonProgress.student_id == child_id,
                    LessonProgress.lesson_id.in_(lessons),
                    LessonProgress.status == "completed",
                )
            )
            if lessons
            else 0
        )
        done = bool(lessons) and done_count == len(lessons)
        state: Literal["done", "pending", "overdue"] = (
            "done" if done else "overdue" if a.due_at is not None and a.due_at < at else "pending"
        )
        out.append(
            AssignmentOut(
                course_id=str(course.id),
                title=course.title,
                classroom=cname,
                due_at=a.due_at,
                status=state,
            )
        )
    return out


def _subjects(service_db: Session, child_id: uuid.UUID, at: datetime) -> list[SubjectCard]:
    course_subject = {
        cid: subj
        for cid, subj in service_db.execute(
            select(Course.id, Course.subject)
            .join(TwinConcept, TwinConcept.course_id == Course.id)
            .where(TwinConcept.student_id == child_id)
            .distinct()
        ).all()
    }
    cards = []
    for subject in sorted(set(course_subject.values())):
        ids = [c for c, s in course_subject.items() if s == subject]
        t = twin.insights(service_db, child_id, at, ids, limit=1)
        strong = t.strongest[0] if t.strongest else None
        weak = t.weakest[0] if t.weakest else None
        if (
            weak is not None
            and strong is not None
            and (weak.kind, weak.key) == (strong.kind, strong.key)
        ):
            weak = None  # only one concept practised so far
        cards.append(
            SubjectCard(
                subject=subject,
                strongest=strong.name if strong else None,
                weakest=weak.name if weak else None,
            )
        )
    return cards


WORDS = {
    "improving": ("improving", "सुधर रही है", "ಸುಧಾರಿಸುತ್ತಿದೆ"),
    "steady": ("steady", "स्थिर है", "ಸ್ಥಿರವಾಗಿದೆ"),
    "needs_practice": ("needs practice", "और अभ्यास चाहिए", "ಇನ್ನಷ್ಟು ಅಭ್ಯಾಸ ಬೇಕು"),
}


def summary(
    name: str,
    week: WeekCard,
    acc: AccuracyCard,
    weakest: dict[str, str] | None,
    pending: int,
    overdue: int,
) -> dict[str, str]:
    """Three short sentences in each language, read aloud by "Listen to summary"."""
    n, last = week.lessons_this_week, week.lessons_last_week
    if n == 0:
        s1 = (
            f"{name} hasn't finished a lesson this week yet.",
            f"{name} ने इस हफ़्ते अभी तक कोई पाठ पूरा नहीं किया है।",
            f"{name} ಈ ವಾರ ಇನ್ನೂ ಯಾವ ಪಾಠವನ್ನೂ ಮುಗಿಸಿಲ್ಲ.",
        )
    else:
        cmp_en = "more than" if n > last else "fewer than" if n < last else "the same as"
        cmp_hi = (
            "पिछले हफ़्ते से ज़्यादा" if n > last else "पिछले हफ़्ते से कम" if n < last else "पिछले हफ़्ते जितने"
        )
        cmp_kn = "ಕಳೆದ ವಾರಕ್ಕಿಂತ ಹೆಚ್ಚು" if n > last else "ಕಳೆದ ವಾರಕ್ಕಿಂತ ಕಡಿಮೆ" if n < last else "ಕಳೆದ ವಾರದಷ್ಟೇ"
        s1 = (
            f"{name} finished {n} lessons this week, {cmp_en} last week.",
            f"{name} ने इस हफ़्ते {n} पाठ पूरे किए, {cmp_hi}।",
            f"{name} ಈ ವಾರ {n} ಪಾಠಗಳನ್ನು ಮುಗಿಸಿದರು, {cmp_kn}.",
        )
    w = WORDS[acc.word]
    if weakest:
        s2 = (
            f"Their accuracy is {w[0]}; {weakest.get('en', '')} needs more practice.",
            f"सही जवाबों की दर {w[1]}; {weakest.get('hi', '')} पर और अभ्यास चाहिए।",
            f"ಸರಿ ಉತ್ತರಗಳ ಪ್ರಮಾಣ {w[2]}; {weakest.get('kn', '')} ಮೇಲೆ ಇನ್ನಷ್ಟು ಅಭ್ಯಾಸ ಬೇಕು.",
        )
    else:
        s2 = (f"Their accuracy is {w[0]}.", f"सही जवाबों की दर {w[1]}।", f"ಸರಿ ಉತ್ತರಗಳ ಪ್ರಮಾಣ {w[2]}.")
    if overdue:
        s3 = (
            f"{overdue} assignments are overdue.",
            f"{overdue} असाइनमेंट की तारीख निकल गई है।",
            f"{overdue} ಅಸೈನ್‌ಮೆಂಟ್‌ಗಳ ಗಡುವು ಮುಗಿದಿದೆ.",
        )
    elif pending:
        s3 = (
            f"{pending} assignments are due soon.",
            f"{pending} असाइनमेंट जल्द पूरे करने हैं।",
            f"{pending} ಅಸೈನ್‌ಮೆಂಟ್‌ಗಳನ್ನು ಶೀಘ್ರದಲ್ಲೇ ಮುಗಿಸಬೇಕು.",
        )
    else:
        s3 = ("All assignments are done.", "सभी असाइनमेंट पूरे हो गए हैं।", "ಎಲ್ಲಾ ಅಸೈನ್‌ಮೆಂಟ್‌ಗಳು ಮುಗಿದಿವೆ.")
    return {lang: " ".join(s[i] for s in (s1, s2, s3)) for i, lang in enumerate(("en", "hi", "kn"))}


@router.get("/parent/children/{child_id}/dashboard")
def child_dashboard(
    child_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb
) -> ChildDashboard:
    child, link = _linked_child(db, service_db, user.id, child_id)
    at = now()
    this_start, this_end = week_bounds(at)
    last_start, last_end = week_bounds(at, 1)

    def completed(start: datetime, end: datetime) -> int:
        return (
            service_db.scalar(
                select(func.count())
                .select_from(LessonProgress)
                .where(
                    LessonProgress.student_id == child.id,
                    LessonProgress.completed_at >= start,
                    LessonProgress.completed_at < end,
                )
            )
            or 0
        )

    attempts = list(
        service_db.scalars(select(AnswerAttempt).where(AnswerAttempt.student_id == child.id))
    )
    n_this, n_last = completed(this_start, this_end), completed(last_start, last_end)
    minutes = sum(a.time_ms for a in attempts if this_start <= a.created_at < this_end) // 60000
    stats = service_db.get(StudentStats, child.id)
    streak = (
        rules.visible_streak(stats.streak, stats.last_active_date, rules.india_today(at))
        if stats
        else 0
    )
    week = WeekCard(
        lessons_this_week=n_this,
        lessons_last_week=n_last,
        arrow="up" if n_this > n_last else "down" if n_this < n_last else "same",
        minutes_this_week=int(minutes),
        streak=streak,
    )
    acc_this = accuracy([a for a in attempts if this_start <= a.created_at < this_end])
    acc_last = accuracy([a for a in attempts if last_start <= a.created_at < last_end])
    acc = AccuracyCard(
        this_week=round(acc_this, 3) if acc_this is not None else None,
        last_week=round(acc_last, 3) if acc_last is not None else None,
        word=twin_model.trend_word(acc_this, acc_last),
    )
    subjects = _subjects(service_db, child.id, at)
    assignments = _assignments(service_db, child.id, at)
    weakest = twin.insights(service_db, child.id, at, limit=1).weakest
    return ChildDashboard(
        child=_child_out(service_db, child, link),
        has_activity=bool(attempts),
        week=week,
        accuracy=acc,
        subjects=subjects,
        assignments=assignments,
        classes=_classes(service_db, child.id),
        summary=summary(
            child.name,
            week,
            acc,
            weakest[0].name if weakest else None,
            sum(1 for a in assignments if a.status == "pending"),
            sum(1 for a in assignments if a.status == "overdue"),
        ),
    )
