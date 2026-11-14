"""Classroom join keys: XXXX-XXXX, e.g. BI09-K4X2.

The first half is the subject code (BI, CH, PH) plus the grade as two digits. The second half
is four random characters from an alphabet without 0, O, 1, I or L, so nothing is misread.
Uniqueness is enforced by the database's unique index; callers retry on a collision.
"""

import re
import secrets
from collections.abc import Callable

ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
SUBJECTS = ("BI", "CH", "PH")
KEY_PATTERN = re.compile(r"^(BI|CH|PH)(0[6-9]|1[0-2])-[" + ALPHABET + r"]{4}$")


def prefix(subject: str, grade: int) -> str:
    if subject not in SUBJECTS:
        raise ValueError(f"unknown subject {subject!r}")
    if not 6 <= grade <= 12:
        raise ValueError(f"grade {grade} outside 6-12")
    return f"{subject}{grade:02d}"


def generate_key(subject: str, grade: int) -> str:
    suffix = "".join(secrets.choice(ALPHABET) for _ in range(4))
    return f"{prefix(subject, grade)}-{suffix}"


def normalize_key(raw: str) -> str | None:
    """Uppercases, strips spaces and inserts the dash. Returns None if it can't be a key."""
    compact = re.sub(r"[\s-]", "", raw).upper()
    if len(compact) != 8:
        return None
    key = f"{compact[:4]}-{compact[4:]}"
    return key if KEY_PATTERN.match(key) else None


def allocate_key(subject: str, grade: int, taken: Callable[[str], bool], retries: int = 20) -> str:
    """Generates keys until one isn't [taken]. Raises if every try collides."""
    for _ in range(retries):
        key = generate_key(subject, grade)
        if not taken(key):
            return key
    raise RuntimeError(f"no free join key for {prefix(subject, grade)} after {retries} tries")
