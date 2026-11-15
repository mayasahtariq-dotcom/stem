"""The Student Twin, stored: updated after every answer, rebuilt from the answer log, and
turned into plain-language insights for teachers, parents and doubt chat.

Only the backend writes the twin (service session). It holds learning data only: no doubt
texts, no AI-written profile of the child.
"""

import statistics
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta
from typing import Any

from sqlalchemy import delete, select
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.orm import Session

from app.db.models import (
    AnswerAttempt,
    Lesson,
    QuestionRow,
    TermRow,
    TwinConcept,
    TwinConfusion,
)
from app.twin import model

LOW_MASTERY = 0.5


def _state(row: TwinConcept | None) -> model.ConceptState:
    if row is None:
        return model.ConceptState()
    return model.ConceptState(
        mastery=row.mastery,
        half_life_days=row.half_life_days,
        attempts=row.attempts,
        correct=row.correct,
        last_seen_at=row.last_seen_at,
    )


def _terms(db: Session, course_id: uuid.UUID) -> dict[str, dict[str, Any]]:
    return {
        t.key: t.content for t in db.scalars(select(TermRow).where(TermRow.course_id == course_id))
    }


def record_attempt(
    db: Session,
    *,
    student_id: uuid.UUID,
    course_id: uuid.UUID,
    question: QuestionRow,
    answer: dict[str, Any],
    correct: bool,
    retry: bool,
    at: datetime,
    terms: dict[str, dict[str, Any]] | None = None,
) -> None:
    """Updates the twin with one answer."""
    terms = terms if terms is not None else _terms(db, course_id)
    ev = model.evidence(
        str(question.lesson_id), question.type, question.content, answer, correct, terms
    )
    for (kind, key), ok in ev.concepts.items():
        row = db.get(TwinConcept, (student_id, course_id, kind, key), populate_existing=True)
        new = model.update(_state(row), ok, ev.guess, at, retry=retry)
        values = {
            "mastery": new.mastery,
            "half_life_days": new.half_life_days,
            "attempts": new.attempts,
            "correct": new.correct,
            "last_seen_at": new.last_seen_at,
            "next_review_at": new.next_review_at(),
        }
        db.execute(
            insert(TwinConcept)
            .values(student_id=student_id, course_id=course_id, kind=kind, key=key, **values)
            .on_conflict_do_update(
                index_elements=[
                    TwinConcept.student_id,
                    TwinConcept.course_id,
                    TwinConcept.kind,
                    TwinConcept.key,
                ],
                set_=values,
            )
        )
    if ev.confusion is not None:
        expected, chosen = ev.confusion
        db.execute(
            insert(TwinConfusion)
            .values(
                student_id=student_id,
                course_id=course_id,
                expected_key=expected,
                chosen_key=chosen,
                count=1,
                last_at=at,
            )
            .on_conflict_do_update(
                index_elements=[
                    TwinConfusion.student_id,
                    TwinConfusion.course_id,
                    TwinConfusion.expected_key,
                    TwinConfusion.chosen_key,
                ],
                set_={"count": TwinConfusion.count + 1, "last_at": at},
            )
        )


def rebuild(db: Session, student_id: uuid.UUID) -> int:
    """Replays the student's whole answer log into a fresh twin. Returns answers replayed."""
    db.execute(delete(TwinConcept).where(TwinConcept.student_id == student_id))
    db.execute(delete(TwinConfusion).where(TwinConfusion.student_id == student_id))
    rows = db.execute(
        select(AnswerAttempt, QuestionRow, Lesson.course_id)
        .join(QuestionRow, QuestionRow.id == AnswerAttempt.question_id)
        .join(Lesson, Lesson.id == AnswerAttempt.lesson_id)
        .where(AnswerAttempt.student_id == student_id)
        .order_by(AnswerAttempt.created_at)
    ).all()
    term_cache: dict[uuid.UUID, dict[str, dict[str, Any]]] = {}
    for attempt, question, course_id in rows:
        if course_id not in term_cache:
            term_cache[course_id] = _terms(db, course_id)
        record_attempt(
            db,
            student_id=student_id,
            course_id=course_id,
            question=question,
            answer=attempt.answer,
            correct=attempt.correct,
            retry=not attempt.first_try,
            at=attempt.created_at,
            terms=term_cache[course_id],
        )
    return len(rows)


# ---- insights ------------------------------------------------------------------------------


@dataclass(frozen=True)
class ConceptInsight:
    kind: str
    key: str
    course_id: str
    name: dict[str, str]
    mastery: float  # shown (after forgetting), 0..1
    attempts: int


@dataclass(frozen=True)
class Confusion:
    expected: dict[str, str]
    chosen: dict[str, str]
    count: int


@dataclass(frozen=True)
class Habits:
    usual_hour: int | None  # India time, 0..23
    median_answer_seconds: float | None
    active_days_14: int


@dataclass(frozen=True)
class TwinInsights:
    weakest: list[ConceptInsight]
    strongest: list[ConceptInsight]
    confusions: list[Confusion]
    habits: Habits

    def low_mastery_keys(self) -> set[tuple[str, str]]:
        return {(c.kind, c.key) for c in self.weakest if c.mastery < LOW_MASTERY}


def concept_names(
    db: Session, course_ids: list[uuid.UUID]
) -> dict[tuple[str, str], dict[str, str]]:
    """Display names: lesson titles and term forms, in all three languages."""
    names: dict[tuple[str, str], dict[str, str]] = {}
    if not course_ids:
        return names
    for lesson in db.scalars(select(Lesson).where(Lesson.course_id.in_(course_ids))):
        names[("lesson", str(lesson.id))] = lesson.title
    for term in db.scalars(select(TermRow).where(TermRow.course_id.in_(course_ids))):
        forms = term.content.get("forms") or {}
        names[("term", term.key)] = {k: str(forms.get(k) or term.key) for k in ("en", "hi", "kn")}
    return names


def insights(
    db: Session,
    student_id: uuid.UUID,
    now: datetime,
    course_ids: list[uuid.UUID] | None = None,
    limit: int = 3,
) -> TwinInsights:
    query = select(TwinConcept).where(TwinConcept.student_id == student_id)
    if course_ids is not None:
        query = query.where(TwinConcept.course_id.in_(course_ids))
    rows = list(db.scalars(query))
    names = concept_names(db, sorted({r.course_id for r in rows}))
    concepts = [
        ConceptInsight(
            kind=r.kind,
            key=r.key,
            course_id=str(r.course_id),
            name=names.get((r.kind, r.key), {"en": r.key, "hi": r.key, "kn": r.key}),
            mastery=round(_state(r).shown(now), 3),
            attempts=r.attempts,
        )
        for r in rows
        if r.attempts > 0
    ]
    by_mastery = sorted(concepts, key=lambda c: (c.mastery, -c.attempts))
    weakest = by_mastery[:limit]
    strongest = sorted(concepts, key=lambda c: (-c.mastery, -c.attempts))[:limit]

    cq = select(TwinConfusion).where(TwinConfusion.student_id == student_id)
    if course_ids is not None:
        cq = cq.where(TwinConfusion.course_id.in_(course_ids))
    confusions = [
        Confusion(
            expected=names.get(("term", c.expected_key), {"en": c.expected_key}),
            chosen=names.get(("term", c.chosen_key), {"en": c.chosen_key}),
            count=c.count,
        )
        for c in sorted(db.scalars(cq), key=lambda c: -c.count)[:limit]
    ]
    return TwinInsights(weakest, strongest, confusions, habits(db, student_id, now))


def habits(db: Session, student_id: uuid.UUID, now: datetime) -> Habits:
    since = now - timedelta(days=14)
    attempts = list(
        db.scalars(
            select(AnswerAttempt).where(
                AnswerAttempt.student_id == student_id, AnswerAttempt.created_at >= since
            )
        )
    )
    if not attempts:
        return Habits(None, None, 0)
    hours = [a.created_at.astimezone(model.INDIA).hour for a in attempts]
    usual = statistics.mode(hours)
    times = [a.time_ms / 1000 for a in attempts if a.time_ms > 0]
    days = {a.created_at.astimezone(model.INDIA).date() for a in attempts}
    return Habits(usual, round(statistics.median(times), 1) if times else None, len(days))


def doubt_context(db: Session, student_id: uuid.UUID, course_id: uuid.UUID, now: datetime) -> str:
    """A short private note for the doubt prompt: what this student finds hard."""
    t = insights(db, student_id, now, [course_id])
    weak = [
        c.name.get("en", c.key) for c in t.weakest if c.kind == "term" and c.mastery < LOW_MASTERY
    ]
    parts = []
    if weak:
        parts.append("finds these hard: " + ", ".join(weak))
    if t.confusions:
        c = t.confusions[0]
        parts.append(f"often confuses {c.expected.get('en')} with {c.chosen.get('en')}")
    return "; ".join(parts)
