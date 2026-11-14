import uuid
from datetime import datetime
from typing import Any

from sqlalchemy import (
    BigInteger,
    CheckConstraint,
    DateTime,
    ForeignKey,
    Identity,
    SmallInteger,
    String,
    Text,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import ARRAY, JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base

ROLES = ("student", "teacher", "parent")
LANGUAGE_MODES = ("hindi", "kannada")


class User(Base):
    __tablename__ = "users"
    __table_args__ = (
        CheckConstraint(
            "roles <@ ARRAY['student','teacher','parent']::text[]", name="users_roles_valid"
        ),
        CheckConstraint("language_mode IN ('hindi','kannada')", name="users_language_mode_valid"),
    )

    # Same ID as the Supabase auth account (auth.users.id).
    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True)
    name: Mapped[str] = mapped_column(Text, nullable=False, server_default="")
    phone: Mapped[str] = mapped_column(String(20), nullable=False, unique=True)
    roles: Mapped[list[str]] = mapped_column(
        ARRAY(Text), nullable=False, server_default=text("'{}'::text[]")
    )
    language_mode: Mapped[str] = mapped_column(Text, nullable=False, server_default="hindi")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class Classroom(Base):
    __tablename__ = "classrooms"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    teacher_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    name: Mapped[str] = mapped_column(Text, nullable=False)
    subject: Mapped[str] = mapped_column(Text, nullable=False)
    grade: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    join_key: Mapped[str] = mapped_column(Text, nullable=False, unique=True)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    allowed_language_modes: Mapped[list[str]] = mapped_column(ARRAY(Text), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class Membership(Base):
    __tablename__ = "memberships"

    classroom_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("classrooms.id", ondelete="CASCADE"), primary_key=True
    )
    student_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), primary_key=True
    )
    joined_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    status: Mapped[str] = mapped_column(Text, nullable=False, server_default="active")


class JoinAttempt(Base):
    __tablename__ = "join_attempts"

    id: Mapped[int] = mapped_column(BigInteger, Identity(always=True), primary_key=True)
    user_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    join_key: Mapped[str] = mapped_column(Text, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


# ---- M2: notes to course -------------------------------------------------------------------


class Upload(Base):
    __tablename__ = "uploads"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    classroom_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("classrooms.id")
    )
    is_personal: Mapped[bool] = mapped_column(nullable=False, server_default=text("false"))
    subject: Mapped[str | None] = mapped_column(Text)
    grade: Mapped[int | None] = mapped_column(SmallInteger)
    page_count: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    status: Mapped[str] = mapped_column(Text, nullable=False, server_default="created")
    error: Mapped[str | None] = mapped_column(Text)
    started_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    finished_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class UploadPage(Base):
    __tablename__ = "upload_pages"

    upload_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("uploads.id"), primary_key=True
    )
    page_index: Mapped[int] = mapped_column(SmallInteger, primary_key=True)
    kind: Mapped[str] = mapped_column(Text, nullable=False)
    storage_path: Mapped[str | None] = mapped_column(Text)
    typed_text: Mapped[str | None] = mapped_column(Text)
    ocr_text: Mapped[str | None] = mapped_column(Text)
    ocr_confidence: Mapped[float | None] = mapped_column()
    diagrams: Mapped[list[dict[str, float]]] = mapped_column(
        JSONB, nullable=False, server_default=text("'[]'::jsonb")
    )


class Course(Base):
    __tablename__ = "courses"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    owner_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"))
    upload_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("uploads.id")
    )
    classroom_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("classrooms.id")
    )
    subject: Mapped[str] = mapped_column(Text, nullable=False)
    grade: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    title: Mapped[dict[str, str]] = mapped_column(JSONB, nullable=False)
    is_personal: Mapped[bool] = mapped_column(nullable=False, server_default=text("false"))
    status: Mapped[str] = mapped_column(Text, nullable=False, server_default="draft")
    prompt_version: Mapped[str] = mapped_column(Text, nullable=False)
    generation_seconds: Mapped[float | None] = mapped_column()
    input_tokens: Mapped[int] = mapped_column(nullable=False, server_default=text("0"))
    output_tokens: Mapped[int] = mapped_column(nullable=False, server_default=text("0"))
    estimated_cost_usd: Mapped[float] = mapped_column(nullable=False, server_default=text("0"))
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )


class Unit(Base):
    __tablename__ = "units"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    course_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("courses.id"))
    position: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    title: Mapped[dict[str, str]] = mapped_column(JSONB, nullable=False)


class Lesson(Base):
    __tablename__ = "lessons"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    course_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("courses.id"))
    unit_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("units.id"))
    position: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    title: Mapped[dict[str, str]] = mapped_column(JSONB, nullable=False)
    summary: Mapped[str] = mapped_column(Text, nullable=False, server_default="")
    generation_status: Mapped[str] = mapped_column(
        Text, nullable=False, server_default="not_started"
    )
    review_status: Mapped[str] = mapped_column(Text, nullable=False, server_default="unreviewed")
    published_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    prompt_version: Mapped[str | None] = mapped_column(Text)


class QuestionRow(Base):
    __tablename__ = "questions"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    lesson_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("lessons.id"))
    position: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    type: Mapped[str] = mapped_column(Text, nullable=False)
    content: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    source_page: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    confidence: Mapped[float] = mapped_column(nullable=False)
    flag_reason: Mapped[str | None] = mapped_column(Text)
    call_safe: Mapped[bool] = mapped_column(nullable=False)
    call_script: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    teacher_edited: Mapped[bool] = mapped_column(nullable=False, server_default=text("false"))
    prompt_version: Mapped[str] = mapped_column(Text, nullable=False)


class TermRow(Base):
    __tablename__ = "terms"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    course_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("courses.id"))
    key: Mapped[str] = mapped_column(Text, nullable=False)
    content: Mapped[dict[str, Any]] = mapped_column(JSONB, nullable=False)
    source_page: Mapped[int] = mapped_column(SmallInteger, nullable=False)
    prompt_version: Mapped[str] = mapped_column(Text, nullable=False)


class CourseAssignment(Base):
    __tablename__ = "course_assignments"

    course_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("courses.id"), primary_key=True
    )
    classroom_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("classrooms.id"), primary_key=True
    )
    due_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    assigned_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
