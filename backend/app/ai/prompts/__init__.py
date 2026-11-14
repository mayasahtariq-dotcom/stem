"""Prompt files. Each starts with `version: <id>` and `---`; every generated row records the
version that made it."""

from dataclasses import dataclass
from functools import lru_cache
from pathlib import Path

_DIR = Path(__file__).parent


@dataclass(frozen=True)
class Prompt:
    version: str
    template: str

    def render(self, **values: str) -> str:
        text = self.template.replace("{rules}", rules())
        for key, value in values.items():
            text = text.replace("{" + key + "}", value)
        return text


@lru_cache
def rules() -> str:
    return (_DIR / "_rules.md").read_text(encoding="utf-8").strip()


@lru_cache
def load(name: str) -> Prompt:
    raw = (_DIR / f"{name}.md").read_text(encoding="utf-8").replace(chr(13) + chr(10), chr(10))
    header, _, body = raw.partition("\n---\n")
    version = header.removeprefix("version:").strip()
    return Prompt(version=version, template=body.strip())
