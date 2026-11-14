"""Text embeddings for searching note chunks (used by doubt chat in M3). The real
implementation is BGE-M3 via sentence-transformers (open source, runs on CPU), installed with
the optional `[ml]` extra."""

import hashlib
import math
from typing import Any, Protocol


class EmbeddingProvider(Protocol):
    dim: int

    def embed(self, texts: list[str]) -> list[list[float]]: ...


class FakeEmbeddingProvider:
    """Deterministic, unit-length vectors derived from a hash of each text."""

    def __init__(self, dim: int = 1024) -> None:
        self.dim = dim

    def embed(self, texts: list[str]) -> list[list[float]]:
        out = []
        for t in texts:
            seed = hashlib.sha256(t.encode("utf-8")).digest()
            raw = [((seed[i % len(seed)] + i * 31) % 255) / 255.0 - 0.5 for i in range(self.dim)]
            norm = math.sqrt(sum(v * v for v in raw)) or 1.0
            out.append([v / norm for v in raw])
        return out


class SentenceTransformerEmbeddingProvider:  # pragma: no cover - needs the [ml] extra
    def __init__(self, model_name: str, dim: int) -> None:
        from sentence_transformers import SentenceTransformer

        self._model: Any = SentenceTransformer(model_name)
        self.dim = dim

    def embed(self, texts: list[str]) -> list[list[float]]:
        vectors = self._model.encode(texts, normalize_embeddings=True)
        return [list(map(float, v)) for v in vectors]
