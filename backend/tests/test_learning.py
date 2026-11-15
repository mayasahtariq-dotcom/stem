"""Student learning (M3) against Postgres: home, path, playing a lesson with server-side
grading, XP, hearts and streaks, term saving, doubts and audio caching."""

from datetime import UTC, datetime, timedelta
from typing import Any

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session

from app.ai.llm import FakeLLMProvider
from app.ai.providers import Services
from app.db.session import act_as
from app.routers import learning
from app.speech.provider import FakeSpeechProvider
from tests.test_courses import make_course, teacher_with_class


@pytest.fixture
def clock(monkeypatch: pytest.MonkeyPatch) -> dict[str, datetime]:
    """Moves the server's clock: set clock["now"]."""
    state = {"now": datetime(2026, 11, 15, 6, 0, tzinfo=UTC)}
    monkeypatch.setattr(learning, "now", lambda: state["now"])
    return state


def class_course_with_student(api: Any, people: Any) -> tuple[Any, Any, dict[str, Any]]:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    student = people("Asha", ["student"])
    assert student.post("/classrooms/join", {"key": classroom["join_key"]}).status_code == 201
    # DPDP (M4): lessons need consent; the school has forms on file for this class.
    consent = teacher.put(f"/classrooms/{classroom['id']}/school-consent", {"enabled": True})
    assert consent.status_code == 200, consent.text
    r = teacher.post(
        f"/lessons/{status['first_lesson_id']}/publish", {"classroom_id": classroom["id"]}
    )
    assert r.status_code == 200, r.text
    return teacher, student, status


def right_answer(q: dict[str, Any]) -> dict[str, Any]:
    c = q["content"]
    if q["type"] == "multiple_choice":
        return {"index": c["correct_index"]}
    if q["type"] == "true_false":
        return {"value": c["answer"]}
    if q["type"] == "fill_formula":
        return {"chip": c["answer"]}
    return {"pairs": [[i, i] for i in range(len(c["pairs"]))], "mistakes": 0}


def wrong_answer(q: dict[str, Any]) -> dict[str, Any]:
    c = q["content"]
    if q["type"] == "multiple_choice":
        return {"index": (c["correct_index"] + 1) % len(c["options"])}
    if q["type"] == "true_false":
        return {"value": not c["answer"]}
    raise AssertionError(q["type"])


def test_home_path_and_continue(api: Any, people: Any, clock: Any) -> None:
    _, student, status = class_course_with_student(api, people)
    home = student.get("/me/home").json()
    assert home["name"] == "Asha"
    assert home["stats"]["hearts"] == 5 and home["stats"]["streak"] == 0
    assert home["stats"]["daily_goal"] == 50
    cont = home["continue_lesson"]
    assert cont["lesson_id"] == status["first_lesson_id"]
    assert cont["lesson_number"] == 1 and cont["lesson_count"] == 1  # only lesson 1 published
    assert home["classes"][0]["teacher_name"] == "Mrs. Rao"
    assert home["classes"][0]["new_lessons"] == 1
    assert home["courses"][0]["percent_complete"] == 0

    path = student.get(f"/courses/{status['course_id']}/path").json()
    states = [ls["state"] for unit in path["units"] for ls in unit["lessons"]]
    assert states == ["current"]  # unpublished lessons stay hidden from students


def test_playing_a_lesson_grades_on_the_server(
    api: Any, people: Any, clock: Any, services: Services
) -> None:
    _, student, status = class_course_with_student(api, people)
    lesson_id = status["first_lesson_id"]
    llm = services.llm
    assert isinstance(llm, FakeLLMProvider)
    questions_before = sum("practice questions" in r.prompt for r in llm.requests)

    started = student.post(f"/lessons/{lesson_id}/start", {})
    assert started.status_code == 200, started.text
    body = started.json()
    questions = body["lesson"]["questions"]
    assert len(questions) == 6 and body["lesson"]["terms"]
    assert all(q["flag_reason"] is None for q in questions)  # review notes stay with the teacher
    # Starting lesson 1 makes lesson 2 (lazy generation, exactly once).
    assert sum("practice questions" in r.prompt for r in llm.requests) == questions_before + 1
    student.post(f"/lessons/{lesson_id}/start", {})
    assert sum("practice questions" in r.prompt for r in llm.requests) == questions_before + 1

    first, second = questions[0], questions[1]
    ok = student.post("/attempts", {"question_id": first["id"], "answer": right_answer(first)})
    assert ok.status_code == 200, ok.text
    assert ok.json()["correct"] and ok.json()["xp_delta"] == 10
    assert ok.json()["stats"]["xp_today"] == 10

    miss = student.post("/attempts", {"question_id": second["id"], "answer": wrong_answer(second)})
    data = miss.json()
    assert not data["correct"] and data["heart_lost"] and data["stats"]["hearts"] == 4
    assert data["correct_answer"] == right_answer(second)
    assert data["stats"]["refill_at"] is not None
    assert data["explanation"]["en"]

    retry = student.post(
        "/attempts",
        {"question_id": second["id"], "answer": right_answer(second), "retry": True},
    )
    assert retry.json()["xp_delta"] == 5

    for q in questions[2:]:
        student.post("/attempts", {"question_id": q["id"], "answer": right_answer(q)})

    done = student.post(f"/lessons/{lesson_id}/complete", {})
    assert done.status_code == 200, done.text
    result = done.json()
    assert result["xp_earned"] == 10 + 5 + 4 * 10 + 20
    assert result["accuracy"] == pytest.approx(5 / 6, abs=0.001)
    assert result["streak"] == 1 and result["streak_extended"] is True
    assert result["stats"]["xp_total"] == 75

    home = student.get("/me/home").json()
    assert home["continue_lesson"] is None  # nothing else published yet
    assert home["courses"][0]["percent_complete"] == 100


def test_bad_answer_shape_is_rejected(api: Any, people: Any, clock: Any) -> None:
    _, student, status = class_course_with_student(api, people)
    q = student.post(f"/lessons/{status['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ][0]
    r = student.post("/attempts", {"question_id": q["id"], "answer": {"nonsense": 1}})
    assert r.status_code == 422 and r.json()["detail"]["code"] == "INVALID_ANSWER"


def test_hearts_run_out_then_refill(api: Any, people: Any, clock: Any) -> None:
    _, student, status = class_course_with_student(api, people)
    questions = student.post(f"/lessons/{status['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ]
    q = questions[0]
    for expected in (4, 3, 2, 1, 0):
        r = student.post("/attempts", {"question_id": q["id"], "answer": wrong_answer(q)})
        assert r.json()["stats"]["hearts"] == expected
    blocked = student.post("/attempts", {"question_id": q["id"], "answer": right_answer(q)})
    assert blocked.status_code == 409
    assert blocked.json()["detail"]["code"] == "HEARTS_EMPTY"
    assert blocked.json()["detail"]["refill_at"]

    clock["now"] += timedelta(hours=4, minutes=1)
    assert student.get("/me/stats").json()["hearts"] == 1
    again = student.post("/attempts", {"question_id": q["id"], "answer": right_answer(q)})
    assert again.status_code == 200


def test_streak_grows_on_consecutive_india_days_and_resets(
    api: Any, people: Any, clock: Any
) -> None:
    _, student, status = class_course_with_student(api, people)
    lesson = status["first_lesson_id"]

    def finish() -> dict[str, Any]:
        student.post(f"/lessons/{lesson}/start", {})
        r = student.post(f"/lessons/{lesson}/complete", {})
        assert r.status_code == 200, r.text
        result: dict[str, Any] = r.json()
        return result

    assert finish()["streak"] == 1
    assert finish()["streak_extended"] is False  # same day
    clock["now"] += timedelta(days=1)
    assert finish()["streak"] == 2
    clock["now"] += timedelta(days=3)
    assert student.get("/me/stats").json()["streak"] == 0  # missed days: shown as broken
    assert finish()["streak"] == 1


def test_complete_needs_a_started_lesson(api: Any, people: Any, clock: Any) -> None:
    _, student, status = class_course_with_student(api, people)
    r = student.post(f"/lessons/{status['first_lesson_id']}/complete", {})
    assert r.status_code == 409 and r.json()["detail"]["code"] == "LESSON_NOT_STARTED"


def test_daily_goal(api: Any, people: Any, clock: Any) -> None:
    student = people("Ravi", ["student"])
    assert student.put("/me/daily-goal", {"goal": 100}).json()["daily_goal"] == 100
    assert student.put("/me/daily-goal", {"goal": 40}).status_code == 422


def test_students_cannot_start_unpublished_lessons(api: Any, people: Any, clock: Any) -> None:
    teacher, student, status = class_course_with_student(api, people)
    course = teacher.get(f"/courses/{status['course_id']}").json()
    second = course["units"][0]["lessons"][1]["id"]
    r = student.post(f"/lessons/{second}/start", {})
    assert r.status_code in (403, 404)


def test_personal_course_lessons_unlock_in_order(api: Any, people: Any, clock: Any) -> None:
    student = people("Meena", ["student"])
    status = make_course(api, student, personal=True)
    path = student.get(f"/courses/{status['course_id']}/path").json()
    lessons = [ls for unit in path["units"] for ls in unit["lessons"]]
    assert [ls["state"] for ls in lessons] == ["current", "locked", "locked"]
    assert lessons[1]["unlocked_by"]["en"] == lessons[0]["title"]["en"]
    assert student.get("/me/home").json()["courses"][0]["ai_made"] is True


def test_saving_terms(api: Any, people: Any, clock: Any) -> None:
    _, student, status = class_course_with_student(api, people)
    course_id = status["course_id"]
    terms = student.get(f"/courses/{course_id}/terms").json()
    assert {t["key"] for t in terms} >= {"mitochondria", "nucleus"}
    assert student.put(f"/me/saved-terms/{course_id}/mitochondria", {}).status_code == 204
    assert student.put(f"/me/saved-terms/{course_id}/mitochondria", {}).status_code == 204
    saved = [t["key"] for t in student.get(f"/courses/{course_id}/terms").json() if t["saved"]]
    assert saved == ["mitochondria"]
    assert student.put(f"/me/saved-terms/{course_id}/no_such_term", {}).status_code == 404
    assert student.delete(f"/me/saved-terms/{course_id}/mitochondria").status_code == 204
    assert not any(t["saved"] for t in student.get(f"/courses/{course_id}/terms").json())


def test_doubts_cite_the_notes_and_have_a_daily_limit(
    api: Any, people: Any, clock: Any, services: Services
) -> None:
    _, student, status = class_course_with_student(api, people)
    course_id = status["course_id"]
    r = student.post("/doubts", {"course_id": course_id, "text": "What do mitochondria do?"})
    assert r.status_code == 200, r.text
    answer = r.json()
    assert answer["in_notes"] is True and answer["source_pages"] == [1]
    assert answer["answer"]["hi"] and answer["doubts_left_today"] == 19

    off = student.post("/doubts", {"course_id": course_id, "text": "Why does the moon shine?"})
    assert off.json()["in_notes"] is False and off.json()["source_pages"] == []
    assert off.json()["answer"]["en"].startswith("Your notes don't cover this")

    services.settings.doubts_per_day = 2
    limited = student.post("/doubts", {"course_id": course_id, "text": "One more?"})
    assert limited.status_code == 429 and limited.json()["detail"]["code"] == "DOUBT_LIMIT"
    clock["now"] += timedelta(days=1)
    assert student.post("/doubts", {"course_id": course_id, "text": "New day"}).status_code == 200


def test_doubt_about_a_question_mentions_it(
    api: Any, people: Any, clock: Any, services: Services
) -> None:
    _, student, status = class_course_with_student(api, people)
    q = student.post(f"/lessons/{status['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ][0]
    r = student.post(
        "/doubts",
        {
            "course_id": status["course_id"],
            "question_id": q["id"],
            "text": "Why?",
            "mode": "simpler",
        },
    )
    assert r.status_code == 200, r.text
    llm = services.llm
    assert isinstance(llm, FakeLLMProvider)
    prompt = llm.requests[-1].prompt
    assert q["content"]["prompt"]["en"] in prompt and "more simply" in prompt


def test_audio_is_made_once_then_cached(api: Any, people: Any, services: Services) -> None:
    student = people("Kiran", ["student"])
    speech = services.speech
    assert isinstance(speech, FakeSpeechProvider)
    first = student.post("/audio", {"text": "माइटोकॉन्ड्रिया", "language": "hi"})
    assert first.status_code == 200, first.text
    second = student.post("/audio", {"text": "माइटोकॉन्ड्रिया", "language": "hi"})
    assert second.status_code == 200
    assert len(speech.synthesized) == 1  # the second request came from the cache
    url = second.json()["url"]
    path = url.split("http://testserver", 1)[1]
    audio = api.get(path)
    assert audio.status_code == 200 and audio.content[:4] == b"RIFF"
    assert audio.headers["content-type"] == "audio/wav"


def test_students_only_see_their_own_attempts(
    api: Any, people: Any, clock: Any, migrated_engine: Engine
) -> None:
    teacher, student, status = class_course_with_student(api, people)
    q = student.post(f"/lessons/{status['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ][0]
    student.post("/attempts", {"question_id": q["id"], "answer": right_answer(q)})
    other = people("Other", ["student"])
    with Session(migrated_engine) as s, s.begin():
        act_as(s, other.id)
        assert s.execute(text("SELECT count(*) FROM answer_attempts")).scalar() == 0
    with Session(migrated_engine) as s, s.begin():
        act_as(s, student.id)
        assert s.execute(text("SELECT count(*) FROM answer_attempts")).scalar() == 1
    with Session(migrated_engine) as s, s.begin():
        act_as(s, teacher.id)  # the course owner sees attempts on their lessons (M4 dashboard)
        assert s.execute(text("SELECT count(*) FROM answer_attempts")).scalar() == 1
