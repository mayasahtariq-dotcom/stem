import pytest

from app.classrooms.keys import ALPHABET, KEY_PATTERN, allocate_key, generate_key, normalize_key


def test_alphabet_has_no_confusable_characters() -> None:
    for ch in "0O1IL":
        assert ch not in ALPHABET


def test_keys_never_use_confusables_and_are_unique_across_10000() -> None:
    issued: set[str] = set()
    for _ in range(10_000):
        key = allocate_key("BI", 9, taken=issued.__contains__)
        assert KEY_PATTERN.match(key), key
        assert not set(key[5:]) & set("0O1IL"), key
        issued.add(key)
    assert len(issued) == 10_000


def test_allocate_key_gives_up_when_everything_is_taken() -> None:
    with pytest.raises(RuntimeError):
        allocate_key("PH", 8, taken=lambda _: True, retries=3)


def test_prefix_encodes_subject_and_grade() -> None:
    assert generate_key("BI", 9).startswith("BI09-")
    assert generate_key("CH", 10).startswith("CH10-")
    assert generate_key("PH", 8).startswith("PH08-")


@pytest.mark.parametrize("subject,grade", [("MA", 9), ("BI", 5), ("BI", 13)])
def test_invalid_prefix_rejected(subject: str, grade: int) -> None:
    with pytest.raises(ValueError):
        generate_key(subject, grade)


@pytest.mark.parametrize(
    "raw,expected",
    [
        ("BI09-K4X2", "BI09-K4X2"),
        ("bi09k4x2", "BI09-K4X2"),
        (" bi09 - k4x2 ", "BI09-K4X2"),
        ("BI09-K4X0", None),  # zero not allowed
        ("BI05-K4X2", None),  # grade outside 6-12
        ("XX09-K4X2", None),
        ("BI09-K4X", None),
    ],
)
def test_normalize_key(raw: str, expected: str | None) -> None:
    assert normalize_key(raw) == expected
