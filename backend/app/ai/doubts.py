"""Doubt chat (PRD M3): short bilingual answers grounded in the teacher's notes, always with
the notes pages they came from, or a clear "your notes don't cover this" first."""

import uuid
from dataclasses import dataclass

from pydantic import BaseModel, ConfigDict, Field
from sqlalchemy import select, text
from sqlalchemy.orm import Session

from app.ai import prompts
from app.ai.pipeline import Usage, _vector, call_json, notes_text
from app.ai.providers import Services
from app.ai.schemas import Tri
from app.db.models import UploadPage

TOP_K = 5

STYLES = {
    "ask": "Answer the doubt directly.",
    "simpler": "Explain it more simply than before, at a lower reading level, with one "
    "everyday analogy.",
    "example": "Give one concrete everyday example that shows the idea.",
}


class DoubtReply(BaseModel):
    model_config = ConfigDict(extra="ignore")
    answer: Tri
    in_notes: bool = True
    source_pages: list[int] = Field(default_factory=list, max_length=10)


@dataclass(frozen=True)
class DoubtResult:
    answer: dict[str, str]
    in_notes: bool
    source_pages: list[int]
    prompt_version: str


def notes_context(services: Services, db: Session, upload_id: uuid.UUID | None, query: str) -> str:
    """The parts of the notes relevant to [query]: nearest chunks by embedding, or the whole
    notes when embeddings are fake (development) or missing. Notes are at most 10 pages."""
    if upload_id is None:
        return ""
    s = services.settings
    if not (s.use_fake_ai or s.fake_embeddings):
        vector = services.embeddings.embed([query])[0]
        rows = db.execute(
            text(
                "SELECT page, text FROM note_chunks WHERE upload_id = :u AND embedding IS NOT NULL"
                " ORDER BY embedding <=> CAST(:e AS vector) LIMIT :k"
            ),
            {"u": upload_id, "e": _vector(vector), "k": TOP_K},
        ).all()
        if rows:
            return "\n\n".join(f"--- Page {r.page} ---\n{r.text}" for r in rows)
    pages = list(db.scalars(select(UploadPage).where(UploadPage.upload_id == upload_id)))
    return notes_text(pages)


def answer_doubt(
    services: Services,
    *,
    notes: str,
    doubt: str,
    about: str,
    language_mode: str,
    grade: int,
    mode: str,
    learner: str = "",
) -> DoubtResult:
    prompt = prompts.load("doubt")
    valid_pages = {int(p) for p in _page_numbers(notes)}
    reply = call_json(
        services,
        services.settings.llm_doubt_model,
        prompt.render(
            language="Hindi" if language_mode == "hindi" else "Kannada",
            grade=str(grade),
            style=STYLES.get(mode, STYLES["ask"]),
            about=about or "the course",
            notes=notes or "(no notes available)",
            doubt=doubt,
            learner=learner or "nothing noted yet",
        ),
        DoubtReply,
        Usage(),
    )
    pages = sorted({p for p in reply.source_pages if p in valid_pages})
    return DoubtResult(reply.answer.model_dump(), reply.in_notes, pages, prompt.version)


def _page_numbers(notes: str) -> list[str]:
    import re

    return re.findall(r"--- Page (\d+) ---", notes)
