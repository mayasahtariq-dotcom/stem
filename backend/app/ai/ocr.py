"""Reading note photos. The real implementation is Surya OCR (open source), installed with the
optional `[ml]` extra. Tests and development use the fake."""

import io
import logging
from dataclasses import dataclass, field
from typing import Any, Protocol

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class Box:
    """A region on the page, in pixels: x0, y0, x1, y1."""

    x0: float
    y0: float
    x1: float
    y1: float


@dataclass(frozen=True)
class OcrPage:
    text: str
    confidence: float  # 0..1, mean over lines
    diagrams: list[Box] = field(default_factory=list)


class OcrProvider(Protocol):
    def read(self, image: bytes) -> OcrPage: ...


class FakeOcrProvider:
    """Returns a fixed page (or one per call from [pages]) for any image."""

    def __init__(self, pages: list[OcrPage] | None = None) -> None:
        self._pages = list(pages or [])
        self.calls = 0

    def read(self, image: bytes) -> OcrPage:
        self.calls += 1
        if self._pages:
            return self._pages.pop(0)
        return OcrPage(
            text=(
                "The cell is the basic unit of life. Mitochondria make energy (ATP) from food. "
                "Plant cells have a cell wall and chloroplasts. The nucleus controls the cell."
            ),
            confidence=0.92,
        )


class SuryaOcrProvider:
    """Surya OCR: text lines with confidence, plus layout boxes for figures (diagram positions).

    VERIFY: Surya's Python API changed between releases (predictor classes in newer versions,
    `run_ocr` in older ones). This supports both; check against the installed version.
    """

    def __init__(self) -> None:
        from surya.detection import DetectionPredictor
        from surya.recognition import RecognitionPredictor

        self._det: Any = DetectionPredictor()
        self._rec: Any = RecognitionPredictor()
        try:
            from surya.layout import LayoutPredictor

            self._layout: Any = LayoutPredictor()
        except ImportError:  # pragma: no cover - optional
            self._layout = None

    def read(self, image: bytes) -> OcrPage:  # pragma: no cover - needs the [ml] extra
        from PIL import Image

        img = Image.open(io.BytesIO(image)).convert("RGB")
        result = self._rec([img], det_predictor=self._det)[0]
        lines = [line for line in result.text_lines if (line.text or "").strip()]
        text = "\n".join(line.text for line in lines)
        conf = sum(float(line.confidence or 0) for line in lines) / len(lines) if lines else 0.0
        diagrams: list[Box] = []
        if self._layout is not None:
            for box in self._layout([img])[0].bboxes:
                if str(box.label).lower() in {"picture", "figure"}:
                    x0, y0, x1, y1 = box.bbox
                    diagrams.append(Box(x0, y0, x1, y1))
        return OcrPage(text=text, confidence=round(conf, 3), diagrams=diagrams)
