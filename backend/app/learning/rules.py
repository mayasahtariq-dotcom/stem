"""Gamification rules and grading (PRD M3). Pure functions: the endpoints load and save the
numbers; everything here is easy to test. All of it runs on the server; the app only shows
the results.

- XP: +10 for a correct first try, +5 for a correct retry, +20 for finishing a lesson.
- Hearts: five at most. A wrong answer costs one, except in match pairs. One heart refills
  every four hours.
- Streak: +1 on any India-time day with at least one finished lesson; a missed day resets it.
- Daily goal: 30, 50 or 100 XP (default 50).
"""

from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Any
from zoneinfo import ZoneInfo

INDIA = ZoneInfo("Asia/Kolkata")

XP_FIRST_TRY = 10
XP_RETRY = 5
XP_LESSON_BONUS = 20
MAX_HEARTS = 5
HEART_REFILL = timedelta(hours=4)
DAILY_GOALS = (30, 50, 100)
DEFAULT_DAILY_GOAL = 50


def india_today(now: datetime) -> date:
    return now.astimezone(INDIA).date()


# ---- hearts --------------------------------------------------------------------------------


@dataclass(frozen=True)
class Hearts:
    count: int
    updated_at: datetime

    @property
    def refill_at(self) -> datetime | None:
        """When the next heart comes back, or None when full."""
        return None if self.count >= MAX_HEARTS else self.updated_at + HEART_REFILL


def refill(hearts: int, updated_at: datetime, now: datetime) -> Hearts:
    """Adds one heart per full four hours since [updated_at]. The leftover time carries over,
    so refills keep their rhythm; when full, the clock restarts at [now]."""
    if hearts >= MAX_HEARTS:
        return Hearts(MAX_HEARTS, now)
    elapsed = now - updated_at
    gained = max(0, int(elapsed / HEART_REFILL))
    if gained == 0:
        return Hearts(hearts, updated_at)
    count = min(MAX_HEARTS, hearts + gained)
    if count >= MAX_HEARTS:
        return Hearts(MAX_HEARTS, now)
    return Hearts(count, updated_at + gained * HEART_REFILL)


def lose_heart(current: Hearts, now: datetime) -> Hearts:
    """One heart fewer. Losing from full starts the refill clock now."""
    if current.count <= 0:
        return current
    started = now if current.count >= MAX_HEARTS else current.updated_at
    return Hearts(current.count - 1, started)


# ---- XP, streak, daily goal ----------------------------------------------------------------


def answer_xp(correct: bool, retry: bool) -> int:
    if not correct:
        return 0
    return XP_RETRY if retry else XP_FIRST_TRY


@dataclass(frozen=True)
class Streak:
    current: int
    longest: int
    last_active: date | None
    extended: bool = False


def visible_streak(streak: int, last_active: date | None, today: date) -> int:
    """The streak to show: it's already broken if the last active day is before yesterday."""
    if last_active is None or last_active < today - timedelta(days=1):
        return 0
    return streak


def finish_lesson_streak(
    streak: int, longest: int, last_active: date | None, today: date
) -> Streak:
    """Updates the streak when a lesson is finished [today] (India time)."""
    if last_active == today:
        return Streak(streak, longest, today, extended=False)
    current = streak + 1 if last_active == today - timedelta(days=1) else 1
    return Streak(current, max(longest, current), today, extended=True)


def xp_today(stored: int, stored_date: date | None, today: date) -> int:
    return stored if stored_date == today else 0


# ---- grading -------------------------------------------------------------------------------


class InvalidAnswerError(ValueError):
    pass


@dataclass(frozen=True)
class Grade:
    correct: bool
    correct_answer: dict[str, Any]
    costs_heart: bool


def grade(question_type: str, content: dict[str, Any], answer: dict[str, Any]) -> Grade:
    """Checks a student's answer against the stored question.

    Answer shapes: multiple_choice {"index": 2}, true_false {"value": true},
    fill_formula {"chip": "F"}, match_pairs {"pairs": [[0, 0], [1, 1], ...], "mistakes": 1}
    (left index → right index, as listed in the question; a match-pairs question is always
    finished by matching everything, and wrong pairs along the way cost no heart).
    """
    try:
        if question_type == "multiple_choice":
            right = int(content["correct_index"])
            return Grade(int(answer["index"]) == right, {"index": right}, costs_heart=True)
        if question_type == "true_false":
            right_tf = bool(content["answer"])
            return Grade(bool(answer["value"]) is right_tf, {"value": right_tf}, costs_heart=True)
        if question_type == "fill_formula":
            right_chip = str(content["answer"])
            return Grade(str(answer["chip"]) == right_chip, {"chip": right_chip}, costs_heart=True)
        if question_type == "match_pairs":
            n = len(content["pairs"])
            expected = [[i, i] for i in range(n)]
            given = sorted([int(a), int(b)] for a, b in answer["pairs"])
            ok = given == expected and int(answer.get("mistakes", 0)) == 0
            return Grade(ok, {"pairs": expected}, costs_heart=False)
    except (KeyError, TypeError, ValueError) as exc:
        raise InvalidAnswerError(str(exc)) from exc
    raise InvalidAnswerError(f"unknown question type {question_type}")
