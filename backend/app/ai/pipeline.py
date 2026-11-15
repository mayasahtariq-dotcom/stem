"""Notes → course pipeline (PRD M2). Each step runs as a background job and records the
upload's status so the app can show progress:

  queued → reading → chunking → outlining → terms → lesson_1 → ready
  (or failed / needs_review)

Generation is lazy: only lesson 1 is made up front; lesson N+1 is made when lesson N is opened
in review (or, from M3, started by a student), exactly once.
"""

import io
import json
import logging
import time
import uuid
from datetime import UTC, datetime
from typing import Any

from pydantic import BaseModel, ValidationError
from sqlalchemy import select, text, update
from sqlalchemy.orm import Session

from app.ai import prompts
from app.ai.llm import LLMError, LLMRequest
from app.ai.providers import Services
from app.ai.schemas import LessonQuestions, Outline, Question, SingleQuestion, TermCards
from app.db.models import Course, Lesson, QuestionRow, TermRow, Unit, Upload, UploadPage
from app.db.session import _engine_for

log = logging.getLogger(__name__)

LOW_OCR_CONFIDENCE = 0.6
CHUNK_CHARS = 800

STEPS = ["queued", "reading", "chunking", "outlining", "terms", "lesson_1", "ready"]


class NeedsReviewError(Exception):
    """The model's output stayed invalid after one retry."""


def register_jobs(services: Services) -> None:
    services.jobs.register("run_upload", lambda upload_id: run_upload(services, upload_id))
    services.jobs.register(
        "generate_lesson", lambda lesson_id: generate_lesson(services, lesson_id)
    )


def _session(services: Services) -> Session:
    return Session(_engine_for(services.settings.database_url), expire_on_commit=False)


def _set_status(
    services: Services, upload_id: uuid.UUID, status: str, error: str | None = None
) -> None:
    with _session(services) as s, s.begin():
        values: dict[str, Any] = {"status": status, "error": error}
        if status == "reading":
            values["started_at"] = datetime.now(UTC)
        if status in ("ready", "failed", "needs_review"):
            values["finished_at"] = datetime.now(UTC)
        s.execute(update(Upload).where(Upload.id == upload_id).values(**values))


# ---- LLM helpers -----------------------------------------------------------------------------


class Usage:
    def __init__(self) -> None:
        self.input_tokens = 0
        self.output_tokens = 0


def call_json[T: BaseModel](
    services: Services, model: str, prompt: str, schema: type[T], usage: Usage
) -> T:
    """Asks for JSON matching [schema]; retries once on invalid output, then NeedsReviewError."""
    system = "You are a careful science teacher's assistant. Reply with one JSON object only."
    last_error = ""
    for attempt in range(2):
        body = (
            prompt
            if attempt == 0
            else (
                f"{prompt}\n\nYour previous reply was not valid ({last_error[:300]}). "
                "Reply again with corrected JSON only."
            )
        )
        try:
            response = services.llm.complete(LLMRequest(model=model, system=system, prompt=body))
        except LLMError as exc:
            last_error = str(exc)
            continue
        usage.input_tokens += response.input_tokens
        usage.output_tokens += response.output_tokens
        try:
            return schema.model_validate_json(_strip_fences(response.text))
        except ValidationError as exc:
            last_error = str(exc)
    raise NeedsReviewError(last_error)


def _strip_fences(raw: str) -> str:
    raw = raw.strip()
    if raw.startswith("```"):
        raw = raw.split("\n", 1)[1] if "\n" in raw else raw
        raw = raw.rsplit("```", 1)[0]
    return raw.strip()


def _cost(services: Services, usage: Usage) -> float:
    s = services.settings
    return (
        usage.input_tokens * s.llm_price_input_per_mtok
        + usage.output_tokens * s.llm_price_output_per_mtok
    ) / 1e6


# ---- notes ---------------------------------------------------------------------------------


def _page_text(services: Services, page: UploadPage) -> tuple[str, float, list[dict[str, float]]]:
    if page.kind == "text":
        return (page.typed_text or "").strip(), 1.0, []
    data = services.storage.read(page.storage_path or "")
    if page.kind == "pdf":
        extracted = _pdf_text(data)
        if len(extracted.strip()) >= 20:
            return extracted, 1.0, []
        images = _pdf_images(data)
        if images:  # a scanned PDF: read each page image
            pages = [services.ocr.read(img) for img in images]
            text_ = "\n\n".join(p.text for p in pages)
            return text_, round(sum(p.confidence for p in pages) / len(pages), 3), []
    result = services.ocr.read(data)
    diagrams = [{"x0": b.x0, "y0": b.y0, "x1": b.x1, "y1": b.y1} for b in result.diagrams]
    return result.text, result.confidence, diagrams


def _pdf_text(data: bytes) -> str:
    try:
        from pypdf import PdfReader

        return "\n".join((p.extract_text() or "") for p in PdfReader(io.BytesIO(data)).pages)
    except Exception:  # noqa: BLE001 - unreadable PDF: fall back to OCR
        return ""


def _pdf_images(data: bytes, max_pages: int = 10) -> list[bytes]:
    """Renders a scanned PDF's pages to JPEG for OCR (needs pypdfium2 from the [ml] extra)."""
    try:
        import pypdfium2
    except ImportError:
        return []
    out: list[bytes] = []
    pdf = pypdfium2.PdfDocument(data)
    for i in range(min(len(pdf), max_pages)):
        image = pdf[i].render(scale=2).to_pil()
        buf = io.BytesIO()
        image.convert("RGB").save(buf, format="JPEG", quality=85)
        out.append(buf.getvalue())
    return out


def notes_text(pages: list[UploadPage]) -> str:
    parts = []
    for p in sorted(pages, key=lambda x: x.page_index):
        header = f"--- Page {p.page_index + 1} ---"
        if (p.ocr_confidence or 1) < LOW_OCR_CONFIDENCE:
            header += f" (hard to read; OCR confidence {p.ocr_confidence:.2f})"
        parts.append(f"{header}\n{p.ocr_text or ''}")
    return "\n\n".join(parts)


def chunk(text_: str, size: int = CHUNK_CHARS) -> list[str]:
    paragraphs = [p.strip() for p in text_.split("\n") if p.strip()]
    chunks: list[str] = []
    current = ""
    for para in paragraphs:
        while len(para) > size:
            chunks.append(para[:size])
            para = para[size:]
        if len(current) + len(para) + 1 > size and current:
            chunks.append(current)
            current = para
        else:
            current = f"{current}\n{para}".strip()
    if current:
        chunks.append(current)
    return chunks


def _vector(values: list[float]) -> str:
    return "[" + ",".join(f"{v:.6f}" for v in values) + "]"


# ---- pipeline ------------------------------------------------------------------------------


def run_upload(services: Services, upload_id: str) -> None:
    uid = uuid.UUID(upload_id)
    started = time.monotonic()
    usage = Usage()
    try:
        _set_status(services, uid, "reading")
        with _session(services) as s, s.begin():
            upload = s.get(Upload, uid)
            if upload is None:
                return
            pages = list(s.scalars(select(UploadPage).where(UploadPage.upload_id == uid)))
            for page in pages:
                page.ocr_text, page.ocr_confidence, page.diagrams = _page_text(services, page)

        _set_status(services, uid, "chunking")
        with _session(services) as s, s.begin():
            for page in pages:
                pieces = chunk(page.ocr_text or "")
                if not pieces:
                    continue
                for piece, vector in zip(pieces, services.embeddings.embed(pieces), strict=True):
                    s.execute(
                        text(
                            "INSERT INTO note_chunks (upload_id, page, text, confidence, embedding)"
                            " VALUES (:u, :p, :t, :c, CAST(:e AS vector))"
                        ),
                        {
                            "u": uid,
                            "p": page.page_index + 1,
                            "t": piece,
                            "c": page.ocr_confidence or 1,
                            "e": _vector(vector),
                        },
                    )

        notes = notes_text(pages)
        if not notes.replace("-", "").strip():
            _set_status(services, uid, "failed", "No readable text found in the notes.")
            return

        _set_status(services, uid, "outlining")
        outline_prompt = prompts.load("outline")
        outline = call_json(
            services,
            services.settings.llm_outline_model,
            outline_prompt.render(
                notes=notes,
                subject=upload.subject or "unknown",
                grade=str(upload.grade or "unknown"),
            ),
            Outline,
            usage,
        )

        with _session(services) as s, s.begin():
            course = Course(
                owner_id=upload.owner_id,
                upload_id=uid,
                classroom_id=upload.classroom_id,
                subject=upload.subject or outline.subject,
                grade=upload.grade or outline.grade,
                title=outline.title.model_dump(),
                is_personal=upload.is_personal,
                status="draft",
                prompt_version=outline_prompt.version,
            )
            s.add(course)
            s.flush()
            lesson_pos = 0
            for u_pos, unit in enumerate(outline.units):
                unit_row = Unit(course_id=course.id, position=u_pos, title=unit.title.model_dump())
                s.add(unit_row)
                s.flush()
                for lesson in unit.lessons:
                    s.add(
                        Lesson(
                            course_id=course.id,
                            unit_id=unit_row.id,
                            position=lesson_pos,
                            title=lesson.title.model_dump(),
                            summary=lesson.summary,
                        )
                    )
                    lesson_pos += 1
            course_id = course.id

        _set_status(services, uid, "terms")
        terms_prompt = prompts.load("terms")
        valid_pages = {p.page_index + 1 for p in pages}
        keys = [t.key for t in outline.key_terms]
        try:
            cards = call_json(
                services,
                services.settings.llm_terms_model,
                terms_prompt.render(notes=notes, keys=", ".join(keys)),
                TermCards,
                usage,
            )
            with _session(services) as s, s.begin():
                seen: set[str] = set()
                for term in cards.terms:
                    if term.key in seen:
                        continue
                    seen.add(term.key)
                    page_no = (
                        term.source_page if term.source_page in valid_pages else min(valid_pages)
                    )
                    s.add(
                        TermRow(
                            course_id=course_id,
                            key=term.key,
                            content=term.model_dump(exclude={"key", "source_page"}),
                            source_page=page_no,
                            prompt_version=terms_prompt.version,
                        )
                    )
        except NeedsReviewError:
            log.warning("term cards need review", extra={"course_id": str(course_id)})

        _set_status(services, uid, "lesson_1")
        with _session(services) as s, s.begin():
            first = s.scalar(
                select(Lesson).where(Lesson.course_id == course_id).order_by(Lesson.position)
            )
            assert first is not None  # noqa: S101 - the outline has at least one lesson
            first.generation_status = "generating"
            first_id = first.id
        _generate_lesson_questions(services, first_id, usage)

        with _session(services) as s, s.begin():
            course_row = s.get(Course, course_id)
            assert course_row is not None  # noqa: S101
            # Personal courses have no review step; their lessons are visible right away.
            if upload.is_personal:
                course_row.status = "published"
                s.execute(
                    update(Lesson)
                    .where(Lesson.course_id == course_id)
                    .values(published_at=datetime.now(UTC))
                )
            else:
                course_row.status = "review"
            course_row.generation_seconds = round(time.monotonic() - started, 2)
            course_row.input_tokens += usage.input_tokens
            course_row.output_tokens += usage.output_tokens
            course_row.estimated_cost_usd += _cost(services, usage)
        log.info(
            "course generated",
            extra={
                "course_id": str(course_id),
                "seconds": round(time.monotonic() - started, 2),
                "input_tokens": usage.input_tokens,
                "output_tokens": usage.output_tokens,
                "estimated_cost_usd": round(_cost(services, usage), 5),
            },
        )
        _set_status(services, uid, "ready")
    except NeedsReviewError as exc:
        _set_status(
            services, uid, "needs_review", f"The AI's reply was not valid: {str(exc)[:500]}"
        )
    except Exception as exc:  # noqa: BLE001 - any failure is recorded for the app to show
        log.exception("pipeline failed", extra={"upload_id": upload_id})
        _set_status(services, uid, "failed", str(exc)[:500])


def generate_lesson(services: Services, lesson_id: str) -> None:
    usage = Usage()
    lid = uuid.UUID(lesson_id)
    _generate_lesson_questions(services, lid, usage)
    with _session(services) as s, s.begin():
        lesson = s.get(Lesson, lid)
        if lesson is not None:
            s.execute(
                update(Course)
                .where(Course.id == lesson.course_id)
                .values(
                    input_tokens=Course.input_tokens + usage.input_tokens,
                    output_tokens=Course.output_tokens + usage.output_tokens,
                    estimated_cost_usd=Course.estimated_cost_usd + _cost(services, usage),
                )
            )


def _lesson_context(
    s: Session, lesson: Lesson
) -> tuple[str, list[str], set[int], dict[int, float]]:
    course = s.get(Course, lesson.course_id)
    assert course is not None  # noqa: S101
    pages = list(s.scalars(select(UploadPage).where(UploadPage.upload_id == course.upload_id)))
    term_keys = list(s.scalars(select(TermRow.key).where(TermRow.course_id == course.id)))
    confidences = {
        p.page_index + 1: (p.ocr_confidence if p.ocr_confidence is not None else 1.0) for p in pages
    }
    return notes_text(pages), term_keys, set(confidences) or {1}, confidences


def _ground(q: Any, valid_pages: set[int], page_conf: dict[int, float]) -> None:
    """Never trust invented page numbers; flag questions from hard-to-read pages."""
    if q.source_page not in valid_pages:
        q.flag_reason = (
            q.flag_reason or ""
        ) + f" The AI cited page {q.source_page}, which isn't in the notes."
        q.source_page = min(valid_pages)
        q.confidence = min(q.confidence, 0.5)
    conf = page_conf.get(q.source_page, 1.0)
    if conf < LOW_OCR_CONFIDENCE and not q.flag_reason:
        q.flag_reason = f"Page {q.source_page} was hard to read; please check this question."
        q.confidence = min(q.confidence, conf)
    if q.flag_reason:
        q.flag_reason = q.flag_reason.strip()


def _question_row(q: Any, lesson_id: uuid.UUID, position: int, version: str) -> QuestionRow:
    content = q.model_dump(
        exclude={"type", "source_page", "confidence", "flag_reason", "call_safe", "call_script"}
    )
    return QuestionRow(
        lesson_id=lesson_id,
        position=position,
        type=q.type,
        content=content,
        source_page=q.source_page,
        confidence=q.confidence,
        flag_reason=q.flag_reason,
        call_safe=q.call_safe,
        call_script=q.call_script.model_dump(),
        prompt_version=version,
    )


def _generate_lesson_questions(services: Services, lesson_id: uuid.UUID, usage: Usage) -> None:
    questions_prompt = prompts.load("questions")
    with _session(services) as s:
        lesson = s.get(Lesson, lesson_id)
        if lesson is None:
            return
        notes, term_keys, valid_pages, page_conf = _lesson_context(s, lesson)
        title = lesson.title.get("en", "")
    try:
        result = call_json(
            services,
            services.settings.llm_questions_model,
            questions_prompt.render(
                notes=notes, lesson_title=title, term_keys=", ".join(term_keys) or "(none)"
            ),
            LessonQuestions,
            usage,
        )
    except NeedsReviewError:
        with _session(services) as s, s.begin():
            s.execute(
                update(Lesson)
                .where(Lesson.id == lesson_id)
                .values(generation_status="needs_review")
            )
        return
    with _session(services) as s, s.begin():
        for pos, q in enumerate(result.questions):
            _ground(q, valid_pages, page_conf)
            s.add(_question_row(q, lesson_id, pos, questions_prompt.version))
        s.execute(
            update(Lesson)
            .where(Lesson.id == lesson_id)
            .values(generation_status="ready", prompt_version=questions_prompt.version)
        )


def claim_upload(services: Services, upload_id: uuid.UUID) -> bool:
    """Moves an upload from created to queued in its own transaction (so the request never
    holds a lock the job needs). Only the first caller wins, so it runs once."""
    with _session(services) as s, s.begin():
        claimed = s.execute(
            update(Upload)
            .where(Upload.id == upload_id, Upload.status == "created")
            .values(status="queued")
            .returning(Upload.id)
        ).scalar()
    return claimed is not None


def ensure_next_lesson(services: Services, lesson_id: uuid.UUID) -> uuid.UUID | None:
    """Starts generating the lesson after [lesson_id], once. Returns the started lesson's id."""
    started = claim_next_lesson(services, lesson_id)
    if started is not None:
        services.jobs.enqueue("generate_lesson", lesson_id=str(started))
    return started


def claim_next_lesson(services: Services, lesson_id: uuid.UUID) -> uuid.UUID | None:
    """Flips the lesson after [lesson_id] from not_started to generating. The conditional
    UPDATE means only the first caller wins, so it's generated exactly once; the caller then
    enqueues the job."""
    with _session(services) as s, s.begin():
        lesson = s.get(Lesson, lesson_id)
        if lesson is None:
            return None
        next_id = s.scalar(
            select(Lesson.id)
            .where(Lesson.course_id == lesson.course_id, Lesson.position > lesson.position)
            .order_by(Lesson.position)
            .limit(1)
        )
        if next_id is None:
            return None
        started = s.execute(
            update(Lesson)
            .where(Lesson.id == next_id, Lesson.generation_status == "not_started")
            .values(generation_status="generating")
            .returning(Lesson.id)
        ).scalar()
    return started


def claim_lesson(services: Services, lesson_id: uuid.UUID) -> bool:
    """Flips [lesson_id] itself from not_started to generating (first caller wins)."""
    with _session(services) as s, s.begin():
        started = s.execute(
            update(Lesson)
            .where(Lesson.id == lesson_id, Lesson.generation_status == "not_started")
            .values(generation_status="generating")
            .returning(Lesson.id)
        ).scalar()
    return started is not None


def regenerate_question(services: Services, question_id: uuid.UUID) -> QuestionRow | None:
    """Replaces one question with a fresh version (teacher's Redo button)."""
    usage = Usage()
    prompt = prompts.load("regenerate")
    with _session(services) as s:
        row = s.get(QuestionRow, question_id)
        if row is None:
            return None
        lesson = s.get(Lesson, row.lesson_id)
        assert lesson is not None  # noqa: S101
        notes, term_keys, valid_pages, page_conf = _lesson_context(s, lesson)
        current = json.dumps({"type": row.type, **row.content}, ensure_ascii=False)
        title = lesson.title.get("en", "")
    result = call_json(
        services,
        services.settings.llm_questions_model,
        prompt.render(
            notes=notes, lesson_title=title, term_keys=", ".join(term_keys), question=current
        ),
        SingleQuestion,
        usage,
    )
    q: Question = result.question
    _ground(q, valid_pages, page_conf)
    new = _question_row(q, row.lesson_id, row.position, prompt.version)
    with _session(services) as s, s.begin():
        target = s.get(QuestionRow, question_id)
        assert target is not None  # noqa: S101
        for field in (
            "type",
            "content",
            "source_page",
            "confidence",
            "flag_reason",
            "call_safe",
            "call_script",
            "prompt_version",
        ):
            setattr(target, field, getattr(new, field))
        target.teacher_edited = False
        s.flush()
        s.refresh(target)
        return target
