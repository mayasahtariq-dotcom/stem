"""The Student Twin's model: pure functions, easy to test and to replay.

Each student has a mastery value per concept (a lesson, or a term of the course):

- Bayesian Knowledge Tracing (BKT) updates mastery after each answer, allowing for lucky
  guesses (1 / number of options) and slips.
- Forgetting: shown mastery fades as p * 2^(-days since practice / half-life). The half-life
  starts at 3 days, doubles after a correct review on a later day (up to 60), and halves after
  a miss (down to 1).
- `next_review_at` is when shown mastery would fall below 0.6 (used by M7's revision).

The twin is a pure function of the answer log, so it can always be rebuilt from attempts.
"""

import math
import re
from dataclasses import dataclass, replace
from datetime import datetime, timedelta
from typing import Any, Literal
from zoneinfo import ZoneInfo

INDIA = ZoneInfo("Asia/Kolkata")

P_INIT = 0.2
P_LEARN = 0.15
P_SLIP = 0.1
HALF_LIFE_START = 3.0
HALF_LIFE_MAX = 60.0
HALF_LIFE_MIN = 1.0
REVIEW_THRESHOLD = 0.6
RETRY_WEIGHT = 0.5


@dataclass(frozen=True)
class ConceptState:
    mastery: float = P_INIT  # at last_seen, before forgetting
    half_life_days: float = HALF_LIFE_START
    attempts: int = 0
    correct: int = 0
    last_seen_at: datetime | None = None

    def shown(self, now: datetime) -> float:
        """Mastery today, after forgetting."""
        return decayed(self.mastery, self.last_seen_at, self.half_life_days, now)

    def next_review_at(self) -> datetime | None:
        if self.last_seen_at is None:
            return None
        if self.mastery <= REVIEW_THRESHOLD:
            return self.last_seen_at
        days = self.half_life_days * math.log2(self.mastery / REVIEW_THRESHOLD)
        return self.last_seen_at + timedelta(days=days)


def decayed(p: float, last_seen: datetime | None, half_life: float, now: datetime) -> float:
    if last_seen is None:
        return p
    days = max(0.0, (now - last_seen).total_seconds() / 86400)
    return p * 2 ** (-days / half_life)


def bkt(p: float, correct: bool, guess: float, weight: float = 1.0) -> float:
    """One BKT step: the posterior given the answer, then a chance of learning."""
    guess = min(max(guess, 0.01), 0.99)
    if correct:
        num = p * (1 - P_SLIP)
        post = num / (num + (1 - p) * guess)
    else:
        num = p * P_SLIP
        post = num / (num + (1 - p) * (1 - guess))
    post = post + (1 - post) * P_LEARN
    return min(1.0, max(0.0, p + weight * (post - p)))


def update(
    state: ConceptState, correct: bool, guess: float, at: datetime, retry: bool = False
) -> ConceptState:
    """The concept's state after one answer at time [at]."""
    prior = state.shown(at)
    weight = RETRY_WEIGHT if retry else 1.0
    mastery = bkt(prior, correct, guess, weight)
    half_life = state.half_life_days
    if correct:
        later_day = state.last_seen_at is not None and (
            state.last_seen_at.astimezone(INDIA).date() < at.astimezone(INDIA).date()
        )
        if later_day:
            half_life = min(HALF_LIFE_MAX, half_life * 2)
    else:
        half_life = max(HALF_LIFE_MIN, half_life / 2)
    return replace(
        state,
        mastery=mastery,
        half_life_days=half_life,
        attempts=state.attempts + 1,
        correct=state.correct + (1 if correct else 0),
        last_seen_at=at,
    )


# ---- what a question is about -------------------------------------------------------------


@dataclass(frozen=True)
class Evidence:
    """One answer, as evidence about concepts: (kind, key) -> correct?, plus a confusion."""

    concepts: dict[tuple[str, str], bool]
    guess: float
    confusion: tuple[str, str] | None = None  # (expected term, chosen term)


def _term_keys_in(text: str, terms: dict[str, dict[str, Any]]) -> set[str]:
    """Course terms named in a piece of English text (whole words, any case)."""
    found: set[str] = set()
    low = text.lower()
    for key, content in terms.items():
        form = str((content.get("forms") or {}).get("en") or "").strip().lower()
        if len(form) >= 3 and re.search(rf"(?<![a-z0-9]){re.escape(form)}(?![a-z0-9])", low):
            found.add(key)
    return found


def _en(value: Any) -> str:
    return str((value or {}).get("en") or "") if isinstance(value, dict) else ""


def evidence(
    lesson_id: str,
    question_type: str,
    content: dict[str, Any],
    answer: dict[str, Any],
    correct: bool,
    terms: dict[str, dict[str, Any]],
) -> Evidence:
    """Which concepts an answer tells us about, and how."""
    concepts: dict[tuple[str, str], bool] = {("lesson", lesson_id): correct}
    prompt_terms = _term_keys_in(_en(content.get("prompt")), terms)
    confusion: tuple[str, str] | None = None
    guess = 0.25
    if question_type == "multiple_choice":
        options = content.get("options") or []
        guess = 1 / max(2, len(options))
        right_i = int(content.get("correct_index", 0))
        right_key = (options[right_i] or {}).get("term_key") if right_i < len(options) else None
        if right_key:
            concepts[("term", right_key)] = correct
        if not correct and right_key:
            chosen = answer.get("index")
            if isinstance(chosen, int) and 0 <= chosen < len(options):
                chosen_key = (options[chosen] or {}).get("term_key")
                if chosen_key and chosen_key != right_key:
                    confusion = (right_key, chosen_key)
    elif question_type == "true_false":
        guess = 0.5
        prompt_terms |= _term_keys_in(_en(content.get("statement")), terms)
    elif question_type == "fill_formula":
        guess = 1 / max(2, len(content.get("chips") or []))
    elif question_type == "match_pairs":
        pairs = content.get("pairs") or []
        guess = 1 / max(2, len(pairs))
        for pair in pairs:
            for side in ("left", "right"):
                key = (pair.get(side) or {}).get("term_key")
                if key:
                    concepts[("term", key)] = correct
    for key in prompt_terms:
        concepts.setdefault(("term", key), correct)
    return Evidence(concepts, guess, confusion)


# ---- plain-language insights ---------------------------------------------------------------


Trend = Literal["improving", "steady", "needs_practice"]


def trend_word(this_week: float | None, last_week: float | None) -> Trend:
    """ "improving", "steady" or "needs_practice" from first-try accuracy this week vs last."""
    if this_week is None:
        return "needs_practice" if last_week is not None else "steady"
    if last_week is None:
        return "steady" if this_week >= 0.6 else "needs_practice"
    if this_week - last_week >= 0.05:
        return "improving"
    if this_week < 0.6 or last_week - this_week >= 0.1:
        return "needs_practice"
    return "steady"
