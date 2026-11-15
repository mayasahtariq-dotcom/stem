"""Gamification rules and grading (pure functions, no database)."""

from datetime import UTC, date, datetime, timedelta

import pytest

from app.learning import rules

T0 = datetime(2026, 11, 15, 6, 0, tzinfo=UTC)


def test_xp_for_answers() -> None:
    assert rules.answer_xp(correct=True, retry=False) == 10
    assert rules.answer_xp(correct=True, retry=True) == 5
    assert rules.answer_xp(correct=False, retry=False) == 0


def test_heart_refills_one_per_four_hours_and_keeps_rhythm() -> None:
    assert rules.refill(3, T0, T0 + timedelta(hours=3, minutes=59)).count == 3
    h = rules.refill(3, T0, T0 + timedelta(hours=5))
    assert h.count == 4 and h.updated_at == T0 + timedelta(hours=4)
    assert h.refill_at == T0 + timedelta(hours=8)
    full = rules.refill(1, T0, T0 + timedelta(hours=40))
    assert full.count == 5 and full.refill_at is None


def test_losing_from_full_starts_the_clock() -> None:
    h = rules.lose_heart(rules.Hearts(5, T0 - timedelta(days=3)), T0)
    assert h.count == 4 and h.refill_at == T0 + timedelta(hours=4)
    again = rules.lose_heart(h, T0 + timedelta(hours=1))
    assert again.count == 3 and again.updated_at == T0  # the running clock continues
    assert rules.lose_heart(rules.Hearts(0, T0), T0).count == 0


def test_streak_counts_india_days() -> None:
    d = date(2026, 11, 15)
    first = rules.finish_lesson_streak(0, 0, None, d)
    assert (first.current, first.extended) == (1, True)
    same_day = rules.finish_lesson_streak(1, 1, d, d)
    assert (same_day.current, same_day.extended) == (1, False)
    next_day = rules.finish_lesson_streak(1, 1, d, d + timedelta(days=1))
    assert (next_day.current, next_day.longest) == (2, 2)
    missed = rules.finish_lesson_streak(5, 5, d, d + timedelta(days=2))
    assert (missed.current, missed.longest) == (1, 5)


def test_visible_streak_breaks_after_a_missed_day() -> None:
    d = date(2026, 11, 15)
    assert rules.visible_streak(4, d, d) == 4
    assert rules.visible_streak(4, d, d + timedelta(days=1)) == 4
    assert rules.visible_streak(4, d, d + timedelta(days=2)) == 0


def test_india_day_boundary() -> None:
    # 19:00 UTC is 00:30 the next day in India.
    assert rules.india_today(datetime(2026, 11, 15, 19, 0, tzinfo=UTC)) == date(2026, 11, 16)


def test_grading_each_type() -> None:
    mcq = {"correct_index": 1}
    assert rules.grade("multiple_choice", mcq, {"index": 1}).correct
    wrong = rules.grade("multiple_choice", mcq, {"index": 0})
    assert not wrong.correct and wrong.costs_heart and wrong.correct_answer == {"index": 1}

    assert rules.grade("true_false", {"answer": False}, {"value": False}).correct
    assert not rules.grade("true_false", {"answer": False}, {"value": True}).correct

    fill = {"answer": "a", "chips": ["a", "m", "F"]}
    assert rules.grade("fill_formula", fill, {"chip": "a"}).correct
    assert not rules.grade("fill_formula", fill, {"chip": "m"}).correct

    pairs: dict[str, list[dict[str, str]]] = {"pairs": [{}, {}, {}]}
    clean = rules.grade("match_pairs", pairs, {"pairs": [[2, 2], [0, 0], [1, 1]], "mistakes": 0})
    assert clean.correct and not clean.costs_heart
    slips = rules.grade("match_pairs", pairs, {"pairs": [[0, 0], [1, 1], [2, 2]], "mistakes": 2})
    assert not slips.correct and not slips.costs_heart  # wrong pairs cost no heart


def test_bad_answers_are_rejected() -> None:
    with pytest.raises(rules.InvalidAnswerError):
        rules.grade("multiple_choice", {"correct_index": 1}, {"chip": "x"})
    with pytest.raises(rules.InvalidAnswerError):
        rules.grade("label_diagram", {}, {})
