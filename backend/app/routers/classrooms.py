"""Classrooms: teachers create and list them; students preview and join with a key."""

import uuid
from datetime import UTC, datetime, timedelta
from typing import Annotated, Literal

from fastapi import APIRouter, Depends, status
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Engine, func, select, text
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.auth import CurrentUser, get_current_user
from app.classrooms.keys import allocate_key, normalize_key
from app.db.models import Classroom, JoinAttempt, Membership, User
from app.db.session import ServiceDb, UserDb, act_as, get_engine
from app.errors import ErrorCode, api_error

router = APIRouter(tags=["classrooms"])

Subject = Literal["BI", "CH", "PH"]
LanguageMode = Literal["hindi", "kannada"]

JOIN_ATTEMPTS_PER_HOUR = 10
KEY_RETRIES = 5

Me = Annotated[CurrentUser, Depends(get_current_user)]


class ClassroomCreate(BaseModel):
    name: str = Field(min_length=1, max_length=60)
    subject: Subject
    grade: int = Field(ge=6, le=12)
    allowed_language_modes: list[LanguageMode] = Field(default=["hindi", "kannada"], min_length=1)

    @field_validator("name")
    @classmethod
    def strip_name(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("name is blank")
        return v

    @field_validator("allowed_language_modes")
    @classmethod
    def dedupe(cls, v: list[LanguageMode]) -> list[LanguageMode]:
        return list(dict.fromkeys(v))


class ClassroomOut(BaseModel):
    id: str
    name: str
    subject: Subject
    grade: int
    join_key: str
    expires_at: datetime | None
    allowed_language_modes: list[LanguageMode]
    student_count: int
    created_at: datetime


class MemberOut(BaseModel):
    id: str
    name: str
    joined_at: datetime


class ClassroomDetail(ClassroomOut):
    members: list[MemberOut]


class ClassroomPreview(BaseModel):
    """Only safe fields: no member list, no teacher phone."""

    id: str
    name: str
    teacher_name: str
    subject: Subject
    grade: int
    student_count: int
    allowed_language_modes: list[LanguageMode]


class JoinRequest(BaseModel):
    key: str = Field(min_length=8, max_length=12)


class StudentClassroomOut(BaseModel):
    id: str
    name: str
    teacher_name: str
    subject: Subject
    grade: int
    student_count: int
    joined_at: datetime


def _out(c: Classroom, count: int) -> ClassroomOut:
    return ClassroomOut(
        id=str(c.id),
        name=c.name,
        subject=c.subject,
        grade=c.grade,
        join_key=c.join_key,
        expires_at=c.expires_at,
        allowed_language_modes=c.allowed_language_modes,
        student_count=count,
        created_at=c.created_at,
    )


def _profile(db: Session, user_id: str) -> User:
    row = db.scalar(select(User).where(User.id == uuid.UUID(user_id)))
    if row is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    return row


def _member_count(db: Session, classroom_id: uuid.UUID) -> int:
    count = db.scalar(text("SELECT app_private.member_count(:cid)"), {"cid": classroom_id})
    return int(count or 0)


# ---- Teachers ---------------------------------------------------------------------------


@router.post("/classrooms", status_code=status.HTTP_201_CREATED)
def create_classroom(body: ClassroomCreate, user: Me, db: UserDb) -> ClassroomOut:
    if "teacher" not in _profile(db, user.id).roles:
        raise api_error(ErrorCode.NOT_A_TEACHER)

    def taken(key: str) -> bool:
        return db.scalar(text("SELECT app_private.join_key_taken(:k)"), {"k": key}) is True

    for _ in range(KEY_RETRIES):
        try:
            key = allocate_key(body.subject, body.grade, taken=taken)
        except RuntimeError:
            break
        classroom = Classroom(
            teacher_id=uuid.UUID(user.id),
            name=body.name,
            subject=body.subject,
            grade=body.grade,
            join_key=key,
            allowed_language_modes=list(body.allowed_language_modes),
        )
        try:
            with db.begin_nested():
                db.add(classroom)
        except IntegrityError:
            continue  # a concurrent insert took the key first: try another
        db.refresh(classroom)
        return _out(classroom, 0)
    raise api_error(ErrorCode.KEY_GENERATION_FAILED)


@router.get("/classrooms")
def list_classrooms(user: Me, db: UserDb) -> list[ClassroomOut]:
    rows = db.scalars(
        select(Classroom)
        .where(Classroom.teacher_id == uuid.UUID(user.id))
        .order_by(Classroom.created_at.desc())
    ).all()
    return [_out(c, _member_count(db, c.id)) for c in rows]


@router.get("/classrooms/{classroom_id}")
def get_classroom(classroom_id: uuid.UUID, user: Me, db: UserDb) -> ClassroomDetail:
    c = db.scalar(select(Classroom).where(Classroom.id == classroom_id))
    # Only the teacher opens a classroom's details; RLS already hides other teachers' rows.
    if c is None or str(c.teacher_id) != user.id:
        raise api_error(ErrorCode.NOT_FOUND)
    members = db.execute(
        select(User.id, User.name, Membership.joined_at)
        .join(Membership, Membership.student_id == User.id)
        .where(Membership.classroom_id == classroom_id, Membership.status == "active")
        .order_by(User.name)
    ).all()
    base = _out(c, len(members))
    return ClassroomDetail(
        **base.model_dump(),
        members=[MemberOut(id=str(m.id), name=m.name, joined_at=m.joined_at) for m in members],
    )


# ---- Students ---------------------------------------------------------------------------


def _check_rate_limit_and_record(engine: Engine, user_id: str, key: str) -> None:
    """Counts this user's attempts in the last hour; records this one in its own transaction
    so it sticks even when the request then fails."""
    with Session(engine) as s, s.begin():
        act_as(s, user_id)
        since = datetime.now(UTC) - timedelta(hours=1)
        recent = s.scalar(
            select(func.count())
            .select_from(JoinAttempt)
            .where(JoinAttempt.user_id == uuid.UUID(user_id), JoinAttempt.created_at > since)
        )
        if (recent or 0) >= JOIN_ATTEMPTS_PER_HOUR:
            raise api_error(ErrorCode.TOO_MANY_ATTEMPTS)
        s.add(JoinAttempt(user_id=uuid.UUID(user_id), join_key=key[:12]))


def _find_joinable(service_db: Session, raw_key: str, student: User) -> tuple[Classroom, str]:
    key = normalize_key(raw_key)
    c = service_db.scalar(select(Classroom).where(Classroom.join_key == key)) if key else None
    if c is None:
        raise api_error(ErrorCode.KEY_NOT_FOUND)
    if c.expires_at is not None and c.expires_at <= datetime.now(UTC):
        raise api_error(ErrorCode.KEY_EXPIRED)
    already = service_db.scalar(
        select(Membership.status).where(
            Membership.classroom_id == c.id, Membership.student_id == student.id
        )
    )
    if already == "active":
        raise api_error(ErrorCode.ALREADY_MEMBER)
    if student.language_mode not in c.allowed_language_modes:
        raise api_error(ErrorCode.LANGUAGE_NOT_ALLOWED)
    teacher_name = service_db.scalar(select(User.name).where(User.id == c.teacher_id)) or ""
    return c, teacher_name


def _student(db: Session, user_id: str) -> User:
    me = _profile(db, user_id)
    if "student" not in me.roles:
        raise api_error(ErrorCode.NOT_A_STUDENT)
    return me


@router.get("/classrooms/preview/{key}")
def preview_classroom(
    key: str,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    engine: Annotated[Engine, Depends(get_engine)],
) -> ClassroomPreview:
    me = _student(db, user.id)
    _check_rate_limit_and_record(engine, user.id, key)
    c, teacher_name = _find_joinable(service_db, key, me)
    count = service_db.scalar(
        select(func.count())
        .select_from(Membership)
        .where(Membership.classroom_id == c.id, Membership.status == "active")
    )
    return ClassroomPreview(
        id=str(c.id),
        name=c.name,
        teacher_name=teacher_name,
        subject=c.subject,
        grade=c.grade,
        student_count=int(count or 0),
        allowed_language_modes=c.allowed_language_modes,
    )


@router.post("/classrooms/join", status_code=status.HTTP_201_CREATED)
def join_classroom(
    body: JoinRequest,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    engine: Annotated[Engine, Depends(get_engine)],
) -> StudentClassroomOut:
    me = _student(db, user.id)
    _check_rate_limit_and_record(engine, user.id, body.key)
    c, teacher_name = _find_joinable(service_db, body.key, me)
    # Insert through the user session so the memberships RLS policy applies.
    db.add(Membership(classroom_id=c.id, student_id=me.id))
    db.flush()
    joined = db.scalar(
        select(Membership.joined_at).where(
            Membership.classroom_id == c.id, Membership.student_id == me.id
        )
    )
    if joined is None:  # pragma: no cover - inserted just above
        raise api_error(ErrorCode.NOT_FOUND)
    return StudentClassroomOut(
        id=str(c.id),
        name=c.name,
        teacher_name=teacher_name,
        subject=c.subject,
        grade=c.grade,
        student_count=_member_count(db, c.id),
        joined_at=joined,
    )


@router.get("/me/classrooms")
def my_classrooms(user: Me, db: UserDb) -> list[StudentClassroomOut]:
    rows = db.execute(
        select(Classroom, Membership.joined_at, User.name)
        .join(Membership, Membership.classroom_id == Classroom.id)
        .join(User, User.id == Classroom.teacher_id)
        .where(Membership.student_id == uuid.UUID(user.id), Membership.status == "active")
        .order_by(Membership.joined_at.desc())
    ).all()
    return [
        StudentClassroomOut(
            id=str(c.id),
            name=c.name,
            teacher_name=teacher_name,
            subject=c.subject,
            grade=c.grade,
            student_count=_member_count(db, c.id),
            joined_at=joined_at,
        )
        for c, joined_at, teacher_name in rows
    ]
