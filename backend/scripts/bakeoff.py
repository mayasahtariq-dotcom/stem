"""Model bake-off: run the real M2 steps on sample note photos with several open models and
save each model's output side by side, so a person can judge which reads and writes best.

Usage (from backend/, with the [ml] extra installed and an OpenAI-compatible key in .env):

    python scripts/bakeoff.py ../samples qwen/qwen-2.5-72b-instruct google/gemma-3-27b-it

Writes docs/demos/M2-samples/<model>/ with ocr.txt, outline.json and lesson1.json, plus
timing and token counts in summary.json. Nothing is written to the database.
"""

import json
import sys
import time
from pathlib import Path

from app.ai import prompts
from app.ai.llm import LLMError, LLMRequest, OpenAICompatibleLLMProvider
from app.ai.schemas import LessonQuestions, Outline
from app.config import get_settings

OUT = Path(__file__).resolve().parents[2] / "docs" / "demos" / "M2-samples"


def read_pages(folder: Path) -> list[tuple[str, str, float]]:
    from app.ai.ocr import SuryaOcrProvider

    ocr = SuryaOcrProvider()
    pages = []
    for image in sorted(
        p for p in folder.iterdir() if p.suffix.lower() in {".jpg", ".jpeg", ".png"}
    ):
        result = ocr.read(image.read_bytes())
        pages.append((image.name, result.text, result.confidence))
    return pages


def notes_text(pages: list[tuple[str, str, float]]) -> str:
    return "\n\n".join(
        f"--- Page {i + 1} --- (OCR confidence {conf:.2f})\n{text}"
        for i, (_, text, conf) in enumerate(pages)
    )


def ask(llm: OpenAICompatibleLLMProvider, model: str, prompt: str) -> tuple[str, int, int, float]:
    started = time.monotonic()
    r = llm.complete(
        LLMRequest(model=model, system="Reply with one JSON object only.", prompt=prompt)
    )
    return r.text, r.input_tokens, r.output_tokens, time.monotonic() - started


def main() -> None:
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    folder = Path(sys.argv[1])
    models = sys.argv[2:]
    settings = get_settings()
    llm = OpenAICompatibleLLMProvider(
        settings.llm_base_url, settings.llm_api_key, settings.llm_timeout_seconds
    )

    print("Reading pages with Surya…")
    t0 = time.monotonic()
    pages = read_pages(folder)
    ocr_seconds = time.monotonic() - t0
    notes = notes_text(pages)
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "ocr.txt").write_text(notes, encoding="utf-8")
    print(
        f"  {len(pages)} pages in {ocr_seconds:.1f}s; mean confidence "
        f"{sum(c for *_, c in pages) / max(len(pages), 1):.2f}"
    )

    for model in models:
        print(f"\n== {model}")
        target = OUT / model.replace("/", "__")
        target.mkdir(parents=True, exist_ok=True)
        summary: dict[str, object] = {"model": model, "ocr_seconds": round(ocr_seconds, 1)}
        try:
            raw, tin, tout, secs = ask(
                llm,
                model,
                prompts.load("outline").render(notes=notes, subject="unknown", grade="unknown"),
            )
            (target / "outline.json").write_text(raw, encoding="utf-8")
            outline = Outline.model_validate_json(raw)
            summary.update(
                outline_valid=True, outline_seconds=round(secs, 1), outline_tokens=[tin, tout]
            )
            first = outline.units[0].lessons[0].title.en
            raw, tin, tout, secs = ask(
                llm,
                model,
                prompts.load("questions").render(
                    notes=notes,
                    lesson_title=first,
                    term_keys=", ".join(t.key for t in outline.key_terms),
                ),
            )
            (target / "lesson1.json").write_text(raw, encoding="utf-8")
            questions = LessonQuestions.model_validate_json(raw)
            summary.update(
                lesson_valid=True,
                lesson_seconds=round(secs, 1),
                lesson_tokens=[tin, tout],
                questions=len(questions.questions),
                flagged=sum(1 for q in questions.questions if q.flag_reason),
            )
        except (LLMError, ValueError) as exc:
            summary["error"] = str(exc)[:500]
        (target / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
        print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
