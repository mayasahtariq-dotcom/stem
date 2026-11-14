"""Notes → course endpoints: uploads and progress, courses, lesson review, question edits,
publishing and assignment."""

import uuid
from datetime import UTC, datetime
from typing import Annotated, Any, Literal

from fastapi import APIRouter, BackgroundTasks, Depends, Request, Response, status
from pydantic import BaseModel, Field, TypeAdapter, ValidationError
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.ai import pipeline
from app.ai.providers import ServicesDep
from app.ai.schemas import Question
from app.auth import CurrentUser, get_current_user
from app.db.models import (
    Classroom,
    Course,
    CourseAssignment,
    Lesson,
    Membership,
    QuestionRow,
    TermRow,
    Unit,
    Upload,
    UploadPage,
    User,
)
from app.db.session import ServiceDb, UserDb
from app.errors import ErrorCode, api_error
from app.storage import LocalStorage

router = APIRouter(tags=["courses"])
Me = Annotated[CurrentUser, Depends(get_current_user)]

MAX_PAGES = 10
MAX_PDF_BYTES = 10 * 1024 * 1024
MAX_PHOTO_BYTES = 12 * 1024 * 1024


# ---- uploads -------------------------------------------------------------------------------


class PageIn(BaseModel):
    kind: Literal["photo", "pdf", "text"]
    text: str | None = Field(default=None, max_length=20_000)
    size_bytes: int | None = Field(default=None, ge=1)


class UploadCreate(BaseModel):
    pages: list[PageIn] = Field(min_length=1, max_length=MAX_PAGES)
    subject: Literal["BI", "CH", "PH"] | None = None
    grade: int | None = Field(default=None, ge=6, le=12)
    classroom_id: uuid.UUID | None = None
    personal: bool = False


class PageTarget(BaseModel):
    index: int
    upload_url: str | None
    method: str = "PUT"
    headers: dict[str, str] = {}


class UploadCreated(BaseModel):
    id: str
    pages: list[PageTarget]


class UploadStatus(BaseModel):
    id: str
    status: str
    steps: list[str]
    page_count: int
    error: str | None
    course_id: str | None
    first_lesson_id: str | None


def _profile(db: Session, user_id: str) -> User:
    me = db.scalar(select(User).where(User.id == uuid.UUID(user_id)))
    if me is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    return me


@router.post("/uploads", status_code=status.HTTP_201_CREATED)
def create_upload(body: UploadCreate, user: Me, db: UserDb, services: ServicesDep) -> UploadCreated:
    me = _profile(db, user.id)
    is_teacher = "teacher" in me.roles
    personal = body.personal or not is_teacher
    if not personal and body.classroom_id is None and not is_teacher:
        raise api_error(ErrorCode.NOT_A_TEACHER)
    if body.classroom_id is not None:
        owns = db.scalar(
            select(Classroom.id).where(
                Classroom.id == body.classroom_id, Classroom.teacher_id == me.id
            )
        )
        if owns is None:
            raise api_error(ErrorCode.NOT_FOUND)
    for page in body.pages:
        if page.kind == "text" and not (page.text or "").strip():
            raise api_error(ErrorCode.INVALID_UPLOAD)
        if page.kind == "pdf" and (page.size_bytes or 0) > MAX_PDF_BYTES:
            raise api_error(ErrorCode.FILE_TOO_LARGE)
        if page.kind == "photo" and (page.size_bytes or 0) > MAX_PHOTO_BYTES:
            raise api_error(ErrorCode.FILE_TOO_LARGE)

    upload = Upload(
        owner_id=me.id,
        classroom_id=None if personal else body.classroom_id,
        is_personal=personal,
        subject=body.subject,
        grade=body.grade,
        page_count=len(body.pages),
    )
    db.add(upload)
    db.flush()
    targets = []
    for index, page in enumerate(body.pages):
        path = None
        target = None
        if page.kind in ("photo", "pdf"):
            ext = "pdf" if page.kind == "pdf" else "jpg"
            path = f"{me.id}/{upload.id}/{index}.{ext}"
            target = services.storage.upload_target(
                path, "application/pdf" if ext == "pdf" else "image/jpeg"
            )
        db.add(
            UploadPage(
                upload_id=upload.id,
                page_index=index,
                kind=page.kind,
                storage_path=path,
                typed_text=page.text,
            )
        )
        targets.append(
            PageTarget(
                index=index,
                upload_url=target.url if target else None,
                method=target.method if target else "PUT",
                headers=target.headers if target else {},
            )
        )
    return UploadCreated(id=str(upload.id), pages=targets)


def _own_upload(db: Session, upload_id: uuid.UUID, user_id: str) -> Upload:
    upload = db.scalar(select(Upload).where(Upload.id == upload_id))
    if upload is None or str(upload.owner_id) != user_id:
        raise api_error(ErrorCode.NOT_FOUND)
    return upload


@router.post("/uploads/{upload_id}/start", status_code=status.HTTP_202_ACCEPTED)
def start_upload(
    upload_id: uuid.UUID, user: Me, db: UserDb, services: ServicesDep, background: BackgroundTasks
) -> UploadStatus:
    upload = _own_upload(db, upload_id, user.id)
    if upload.status != "created":
        return _status(db, upload)
    pages = db.scalars(select(UploadPage).where(UploadPage.upload_id == upload_id)).all()
    missing = [
        p.page_index
        for p in pages
        if p.storage_path and not services.storage.exists(p.storage_path)
    ]
    if missing:
        raise api_error(ErrorCode.PAGES_MISSING)
    # Claimed in a separate, already-committed transaction; this request only reads the upload,
    # so the job (run after the response) never waits on a lock held here.
    if pipeline.claim_upload(services, upload_id):
        background.add_task(services.jobs.enqueue, "run_upload", upload_id=str(upload_id))
    status_out = _status(db, upload)
    return status_out.model_copy(update={"status": "queued", "steps": ["queued"]})


def _status(db: Session, upload: Upload) -> UploadStatus:
    course = db.scalar(select(Course).where(Course.upload_id == upload.id))
    first = None
    if course is not None:
        first = db.scalar(
            select(Lesson.id).where(Lesson.course_id == course.id).order_by(Lesson.position)
        )
    done = (
        pipeline.STEPS[: pipeline.STEPS.index(upload.status) + 1]
        if upload.status in pipeline.STEPS
        else []
    )
    return UploadStatus(
        id=str(upload.id),
        status=upload.status,
        steps=done,
        page_count=upload.page_count,
        error=upload.error,
        course_id=str(course.id) if course else None,
        first_lesson_id=str(first) if first else None,
    )


@router.get("/uploads/{upload_id}")
def get_upload(upload_id: uuid.UUID, user: Me, db: UserDb) -> UploadStatus:
    return _status(db, _own_upload(db, upload_id, user.id))


@router.put(
    "/local-storage/{token}", status_code=status.HTTP_204_NO_CONTENT, include_in_schema=False
)
async def local_storage_put(token: str, request: Request, services: ServicesDep) -> Response:
    """Development storage: receives a page upload for a link we signed."""
    storage = services.storage
    if not isinstance(storage, LocalStorage):
        raise api_error(ErrorCode.NOT_FOUND)
    path = storage.path_from_token(token)
    if path is None:
        raise api_error(ErrorCode.NOT_FOUND)
    data = await request.body()
    if len(data) > MAX_PHOTO_BYTES:
        raise api_error(ErrorCode.FILE_TOO_LARGE)
    storage.write(path, data)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


# ---- courses -------------------------------------------------------------------------------


class LessonSummary(BaseModel):
    id: str
    position: int
    title: dict[str, str]
    generation_status: str
    published: bool
    question_count: int
    flagged_count: int


class UnitOut(BaseModel):
    id: str
    title: dict[str, str]
    lessons: list[LessonSummary]


class CourseSummary(BaseModel):
    id: str
    title: dict[str, str]
    subject: str
    grade: int
    status: str
    is_personal: bool
    ai_made: bool
    lesson_count: int
    published_count: int


class CourseOut(CourseSummary):
    units: list[UnitOut]
    assigned_classroom_ids: list[str]
    question_count: int
    edited_count: int


def _course_summary(
    db: Session, c: Course, visible_lessons: list[Lesson] | None = None
) -> CourseSummary:
    lessons = (
        visible_lessons
        if visible_lessons is not None
        else list(db.scalars(select(Lesson).where(Lesson.course_id == c.id)))
    )
    return CourseSummary(
        id=str(c.id),
        title=c.title,
        subject=c.subject,
        grade=c.grade,
        status=c.status,
        is_personal=c.is_personal,
        ai_made=c.is_personal,
        lesson_count=len(lessons),
        published_count=sum(1 for lesson in lessons if lesson.published_at is not None),
    )


@router.get("/courses")
def teacher_courses(user: Me, db: UserDb) -> list[CourseSummary]:
    rows = db.scalars(
        select(Course)
        .where(Course.owner_id == uuid.UUID(user.id))
        .order_by(Course.created_at.desc())
    ).all()
    return [_course_summary(db, c) for c in rows]


@router.get("/me/courses")
def student_courses(user: Me, db: UserDb) -> list[CourseSummary]:
    """Personal courses plus class courses assigned to the student's classrooms that have at
    least one published lesson."""
    me = uuid.UUID(user.id)
    personal = db.scalars(
        select(Course).where(Course.owner_id == me, Course.is_personal.is_(True))
    ).all()
    assigned = db.scalars(
        select(Course)
        .join(CourseAssignment, CourseAssignment.course_id == Course.id)
        .join(Membership, Membership.classroom_id == CourseAssignment.classroom_id)
        .where(Membership.student_id == me, Membership.status == "active")
        .distinct()
    ).all()
    out = [_course_summary(db, c) for c in personal]
    for c in assigned:
        published = list(
            db.scalars(
                select(Lesson).where(Lesson.course_id == c.id, Lesson.published_at.is_not(None))
            )
        )
        if published:
            out.append(_course_summary(db, c, published))
    return out


def _readable_course(
    db: Session, service_db: Session, course_id: uuid.UUID, user_id: str
) -> tuple[Course, bool]:
    """Loads the course through the user's session, so RLS decides visibility (owners and
    students in an assigned classroom); anyone else gets 404."""
    course = db.scalar(select(Course).where(Course.id == course_id))
    if course is None:
        raise api_error(ErrorCode.NOT_FOUND)
    return course, str(course.owner_id) == user_id


@router.get("/courses/{course_id}")
def get_course(course_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb) -> CourseOut:
    course, is_owner = _readable_course(db, service_db, course_id, user.id)
    units = db.scalars(
        select(Unit).where(Unit.course_id == course_id).order_by(Unit.position)
    ).all()
    lessons = db.scalars(
        select(Lesson).where(Lesson.course_id == course_id).order_by(Lesson.position)
    ).all()
    counts = dict(
        db.execute(
            select(QuestionRow.lesson_id, func.count())
            .where(QuestionRow.lesson_id.in_([lesson.id for lesson in lessons]))
            .group_by(QuestionRow.lesson_id)
        ).all()
    )
    flagged = dict(
        db.execute(
            select(QuestionRow.lesson_id, func.count())
            .where(
                QuestionRow.lesson_id.in_([lesson.id for lesson in lessons]),
                QuestionRow.flag_reason.is_not(None),
            )
            .group_by(QuestionRow.lesson_id)
        ).all()
    )
    edited = (
        db.scalar(
            select(func.count()).where(
                QuestionRow.lesson_id.in_([lesson.id for lesson in lessons]),
                QuestionRow.teacher_edited.is_(True),
            )
        )
        or 0
    )
    assigned = [
        str(c)
        for c in db.scalars(
            select(CourseAssignment.classroom_id).where(CourseAssignment.course_id == course_id)
        )
    ]
    unit_out = [
        UnitOut(
            id=str(u.id),
            title=u.title,
            lessons=[
                LessonSummary(
                    id=str(lesson.id),
                    position=lesson.position,
                    title=lesson.title,
                    generation_status=lesson.generation_status,
                    published=lesson.published_at is not None,
                    question_count=int(counts.get(lesson.id, 0)),
                    flagged_count=int(flagged.get(lesson.id, 0)) if is_owner else 0,
                )
                for lesson in lessons
                if lesson.unit_id == u.id
            ],
        )
        for u in units
    ]
    summary = _course_summary(db, course, list(lessons))
    return CourseOut(
        **summary.model_dump(),
        units=unit_out,
        assigned_classroom_ids=assigned if is_owner else [],
        question_count=sum(int(v) for v in counts.values()),
        edited_count=int(edited),
    )


class TitleUpdate(BaseModel):
    title: dict[str, str]


@router.put("/courses/{course_id}")
def update_course_title(
    course_id: uuid.UUID, body: TitleUpdate, user: Me, db: UserDb, service_db: ServiceDb
) -> CourseOut:
    course, is_owner = _readable_course(db, service_db, course_id, user.id)
    if not is_owner:
        raise api_error(ErrorCode.NOT_FOUND)
    row = db.get(Course, course_id)
    assert row is not None  # noqa: S101
    row.title = {
        **row.title,
        **{k: v.strip() for k, v in body.title.items() if k in ("en", "hi", "kn") and v.strip()},
    }
    db.flush()
    return get_course(course_id, user, db, service_db)


# ---- lessons and questions -----------------------------------------------------------------


class QuestionOut(BaseModel):
    id: str
    position: int
    type: str
    content: dict[str, Any]
    source_page: int
    confidence: float
    flag_reason: str | None
    call_safe: bool
    teacher_edited: bool


class LessonOut(BaseModel):
    id: str
    course_id: str
    title: dict[str, str]
    generation_status: str
    published: bool
    questions: list[QuestionOut]
    terms: dict[str, dict[str, Any]]


def _question_out(q: QuestionRow, owner: bool) -> QuestionOut:
    return QuestionOut(
        id=str(q.id),
        position=q.position,
        type=q.type,
        content=q.content,
        source_page=q.source_page,
        confidence=q.confidence,
        flag_reason=q.flag_reason if owner else None,
        call_safe=q.call_safe,
        teacher_edited=q.teacher_edited,
    )


@router.get("/lessons/{lesson_id}")
def get_lesson(
    lesson_id: uuid.UUID,
    user: Me,
    db: UserDb,
    service_db: ServiceDb,
    services: ServicesDep,
    background: BackgroundTasks,
) -> LessonOut:
    lesson = service_db.get(Lesson, lesson_id)
    if lesson is None:
        raise api_error(ErrorCode.NOT_FOUND)
    course, is_owner = _readable_course(db, service_db, lesson.course_id, user.id)
    if not is_owner and lesson.published_at is None:
        raise api_error(ErrorCode.LESSON_NOT_PUBLISHED)
    questions = db.scalars(
        select(QuestionRow).where(QuestionRow.lesson_id == lesson_id).order_by(QuestionRow.position)
    ).all()
    terms = {
        t.key: t.content for t in db.scalars(select(TermRow).where(TermRow.course_id == course.id))
    }
    if is_owner:
        # Opening lesson N in review triggers generation of lesson N+1 (lazy, once).
        started = pipeline.claim_next_lesson(services, lesson_id)
        if started is not None:
            background.add_task(services.jobs.enqueue, "generate_lesson", lesson_id=str(started))
    return LessonOut(
        id=str(lesson.id),
        course_id=str(course.id),
        title=lesson.title,
        generation_status=lesson.generation_status,
        published=lesson.published_at is not None,
        questions=[_question_out(q, is_owner) for q in questions],
        terms=terms,
    )


def _owned_question(
    db: Session, service_db: Session, question_id: uuid.UUID, user_id: str
) -> QuestionRow:
    q = service_db.get(QuestionRow, question_id)
    if q is None:
        raise api_error(ErrorCode.NOT_FOUND)
    lesson = service_db.get(Lesson, q.lesson_id)
    course = service_db.get(Course, lesson.course_id) if lesson else None
    if course is None or str(course.owner_id) != user_id:
        raise api_error(ErrorCode.NOT_FOUND)
    row = db.get(QuestionRow, question_id)
    if row is None:
        raise api_error(ErrorCode.NOT_FOUND)
    return row


_question_adapter: TypeAdapter[Question] = TypeAdapter(Question)


class QuestionUpdate(BaseModel):
    question: dict[str, Any]


@router.put("/questions/{question_id}")
def update_question(
    question_id: uuid.UUID, body: QuestionUpdate, user: Me, db: UserDb, service_db: ServiceDb
) -> QuestionOut:
    row = _owned_question(db, service_db, question_id, user.id)
    merged = {
        "source_page": row.source_page,
        "confidence": 1.0,
        "flag_reason": None,
        "call_safe": row.call_safe,
        "call_script": row.call_script,
        **body.question,
    }
    try:
        q = _question_adapter.validate_python(merged)
    except ValidationError as exc:
        raise api_error(ErrorCode.INVALID_QUESTION) from exc
    row.type = q.type
    row.content = q.model_dump(
        exclude={"type", "source_page", "confidence", "flag_reason", "call_safe", "call_script"}
    )
    row.call_safe = q.call_safe
    row.call_script = q.call_script.model_dump()
    row.flag_reason = None
    row.confidence = 1.0
    row.teacher_edited = True
    db.flush()
    return _question_out(row, owner=True)


@router.post("/questions/{question_id}/regenerate")
def regenerate(
    question_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb, services: ServicesDep
) -> QuestionOut:
    _owned_question(db, service_db, question_id, user.id)
    try:
        row = pipeline.regenerate_question(services, question_id)
    except pipeline.NeedsReviewError as exc:
        raise api_error(ErrorCode.AI_UNAVAILABLE) from exc
    if row is None:
        raise api_error(ErrorCode.NOT_FOUND)
    return _question_out(row, owner=True)


@router.delete("/questions/{question_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_question(
    question_id: uuid.UUID, user: Me, db: UserDb, service_db: ServiceDb
) -> Response:
    row = _owned_question(db, service_db, question_id, user.id)
    db.delete(row)
    return Response(status_code=status.HTTP_204_NO_CONTENT)


# ---- publishing and assignment ---------------------------------------------------------------


class PublishRequest(BaseModel):
    classroom_id: uuid.UUID | None = None


def _own_classroom(db: Session, classroom_id: uuid.UUID, user_id: str) -> None:
    if (
        db.scalar(
            select(Classroom.id).where(
                Classroom.id == classroom_id, Classroom.teacher_id == uuid.UUID(user_id)
            )
        )
        is None
    ):
        raise api_error(ErrorCode.NOT_FOUND)


@router.post("/lessons/{lesson_id}/publish")
def publish_lesson(
    lesson_id: uuid.UUID, body: PublishRequest, user: Me, db: UserDb, service_db: ServiceDb
) -> LessonSummary:
    lesson = service_db.get(Lesson, lesson_id)
    if lesson is None:
        raise api_error(ErrorCode.NOT_FOUND)
    course, is_owner = _readable_course(db, service_db, lesson.course_id, user.id)
    if not is_owner:
        raise api_error(ErrorCode.NOT_FOUND)
    if lesson.generation_status != "ready":
        raise api_error(ErrorCode.LESSON_NOT_READY)
    classroom_id = body.classroom_id or course.classroom_id
    if classroom_id is not None and not course.is_personal:
        _own_classroom(db, classroom_id, user.id)
        if db.get(CourseAssignment, (course.id, classroom_id)) is None:
            db.add(CourseAssignment(course_id=course.id, classroom_id=classroom_id))
    row = db.get(Lesson, lesson_id)
    assert row is not None  # noqa: S101
    row.published_at = row.published_at or datetime.now(UTC)
    row.review_status = "reviewed"
    course_row = db.get(Course, course.id)
    assert course_row is not None  # noqa: S101
    course_row.status = "published"
    db.flush()
    count = db.scalar(select(func.count()).where(QuestionRow.lesson_id == lesson_id)) or 0
    return LessonSummary(
        id=str(row.id),
        position=row.position,
        title=row.title,
        generation_status=row.generation_status,
        published=True,
        question_count=int(count),
        flagged_count=0,
    )


class AssignRequest(BaseModel):
    classroom_ids: list[uuid.UUID] = Field(min_length=1)
    due_at: datetime | None = None


@router.post("/courses/{course_id}/assign")
def assign_course(
    course_id: uuid.UUID, body: AssignRequest, user: Me, db: UserDb, service_db: ServiceDb
) -> CourseOut:
    course, is_owner = _readable_course(db, service_db, course_id, user.id)
    if not is_owner or course.is_personal:
        raise api_error(ErrorCode.NOT_FOUND)
    for cid in body.classroom_ids:
        _own_classroom(db, cid, user.id)
        existing = db.get(CourseAssignment, (course_id, cid))
        if existing is None:
            db.add(CourseAssignment(course_id=course_id, classroom_id=cid, due_at=body.due_at))
        else:
            existing.due_at = body.due_at
    db.flush()
    return get_course(course_id, user, db, service_db)
