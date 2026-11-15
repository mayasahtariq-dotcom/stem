"""The Student Twin's model, checked against hand calculations."""

from datetime import UTC, datetime, timedelta

import pytest

from app.twin import model

T0 = datetime(2026, 11, 16, 6, 0, tzinfo=UTC)  # 11:30 in India
TERMS = {
    "mitochondria": {"forms": {"en": "Mitochondria"}},
    "nucleus": {"forms": {"en": "Nucleus"}},
    "cell_wall": {"forms": {"en": "Cell wall"}},
}


def test_bkt_matches_hand_calculation() -> None:
    # Correct, p=0.2, guess=1/3: post = 0.18 / (0.18 + 0.8/3) = 0.40299; + learning 0.15
    p = model.bkt(0.2, True, 1 / 3)
    assert p == pytest.approx(0.40299 + (1 - 0.40299) * 0.15, abs=1e-4)
    # Wrong, p=0.5, guess=0.5: post = 0.05 / (0.05 + 0.25) = 0.16667; + learning
    assert model.bkt(0.5, False, 0.5) == pytest.approx(0.16667 + 0.83333 * 0.15, abs=1e-4)
    # A retry moves half as far.
    full = model.bkt(0.2, True, 1 / 3)
    assert model.bkt(0.2, True, 1 / 3, weight=0.5) == pytest.approx(0.2 + (full - 0.2) / 2)


def test_forgetting_halves_per_half_life() -> None:
    s = model.ConceptState(mastery=0.8, half_life_days=3, last_seen_at=T0)
    assert s.shown(T0) == pytest.approx(0.8)
    assert s.shown(T0 + timedelta(days=3)) == pytest.approx(0.4)
    # 0.8 * 2^(-t/3) = 0.6  ->  t = 3 * log2(4/3) = 1.245 days
    review = s.next_review_at()
    assert review is not None
    assert (review - T0).total_seconds() / 86400 == pytest.approx(1.2451, abs=1e-3)


def test_half_life_grows_on_later_day_success_and_shrinks_on_miss() -> None:
    s = model.update(model.ConceptState(), True, 1 / 3, T0)
    assert s.half_life_days == 3  # first sight: no review yet
    same_day = model.update(s, True, 1 / 3, T0 + timedelta(hours=1))
    assert same_day.half_life_days == 3
    next_day = model.update(s, True, 1 / 3, T0 + timedelta(days=1))
    assert next_day.half_life_days == 6
    miss = model.update(next_day, False, 1 / 3, T0 + timedelta(days=2))
    assert miss.half_life_days == 3
    assert (miss.attempts, miss.correct) == (3, 2)


def test_mastery_rises_with_practice() -> None:
    s = model.ConceptState()
    for i in range(5):
        s = model.update(s, True, 1 / 3, T0 + timedelta(minutes=i))
    assert s.mastery > 0.95


def test_evidence_from_a_multiple_choice_miss_records_the_confusion() -> None:
    content = {
        "prompt": {"en": "Which part of the cell makes energy?"},
        "options": [
            {"term_key": "nucleus"},
            {"term_key": "mitochondria"},
            {"text": {"en": "Ribosome"}},
        ],
        "correct_index": 1,
    }
    ev = model.evidence("L1", "multiple_choice", content, {"index": 0}, False, TERMS)
    assert ev.concepts == {("lesson", "L1"): False, ("term", "mitochondria"): False}
    assert ev.confusion == ("mitochondria", "nucleus")
    assert ev.guess == pytest.approx(1 / 3)


def test_evidence_finds_terms_in_true_false_statements_and_match_pairs() -> None:
    tf = {"prompt": {"en": "True or false?"}, "statement": {"en": "Plant cells have a cell wall."}}
    ev = model.evidence("L1", "true_false", tf, {"value": True}, True, TERMS)
    assert ev.concepts[("term", "cell_wall")] is True and ev.guess == 0.5
    pairs = {"pairs": [{"left": {"term_key": "nucleus"}, "right": {"text": {"en": "Controls"}}}]}
    ev2 = model.evidence(
        "L1", "match_pairs", pairs, {"pairs": [[0, 0]], "mistakes": 0}, True, TERMS
    )
    assert ev2.concepts[("term", "nucleus")] is True


def test_trend_words() -> None:
    assert model.trend_word(0.8, 0.6) == "improving"
    assert model.trend_word(0.7, 0.72) == "steady"
    assert model.trend_word(0.5, 0.55) == "needs_practice"
    assert model.trend_word(None, None) == "steady"
