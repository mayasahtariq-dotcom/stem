"""Notes → course: the pipeline with fake AI, lazy generation, review edits, publishing and
who can see what (needs Postgres with pgvector)."""

import json
from typing import Any

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session

from app.ai import pipeline
from app.ai.fake_content import fake_llm_response
from app.ai.llm import FakeLLMProvider, LLMRequest
from app.ai.providers import Services
from app.db.session import act_as

PHOTO = b"\xff\xd8\xff" + b"fake-jpeg-bytes" * 50


def teacher_with_class(people: Any) -> tuple[Any, dict[str, Any]]:
    teacher = people("Mrs. Rao", ["teacher"])
    r = teacher.post("/classrooms", {"name": "Class 9-A", "subject": "BI", "grade": 9})
    assert r.status_code == 201, r.text
    classroom: dict[str, Any] = r.json()
    return teacher, classroom


def make_course(
    api: Any, owner: Any, classroom_id: str | None = None, personal: bool = False
) -> dict[str, Any]:
    body: dict[str, Any] = {
        "pages": [
            {"kind": "photo", "size_bytes": len(PHOTO)},
            {"kind": "text", "text": "Chloroplasts make food."},
        ],
        "subject": "BI",
        "grade": 9,
        "personal": personal,
    }
    if classroom_id:
        body["classroom_id"] = classroom_id
    created = owner.post("/uploads", body)
    assert created.status_code == 201, created.text
    upload = created.json()
    photo = upload["pages"][0]
    assert upload["pages"][1]["upload_url"] is None  # typed text needs no upload
    put = api.put(photo["upload_url"], content=PHOTO, headers=photo["headers"])
    assert put.status_code == 204, put.text
    started = owner.post(f"/uploads/{upload['id']}/start", {})
    assert started.status_code == 202, started.text
    status: dict[str, Any] = owner.get(f"/uploads/{upload['id']}").json()
    return status


# ---- pipeline ------------------------------------------------------------------------------


def test_pipeline_statuses_move_in_order(
    people: Any, api: Any, monkeypatch: pytest.MonkeyPatch
) -> None:
    seen: list[str] = []
    real = pipeline._set_status

    def record(services: Services, upload_id: Any, status: str, error: str | None = None) -> None:
        seen.append(status)
        real(services, upload_id, status, error)

    monkeypatch.setattr(pipeline, "_set_status", record)
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    assert seen == ["reading", "chunking", "outlining", "terms", "lesson_1", "ready"]
    assert status["status"] == "ready"
    assert status["steps"] == pipeline.STEPS
    assert status["course_id"] and status["first_lesson_id"]


def test_course_outline_terms_and_first_lesson(
    people: Any, api: Any, migrated_engine: Engine
) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    course = teacher.get(f"/courses/{status['course_id']}").json()
    assert course["title"]["en"] == "The Cell" and course["status"] == "review"
    lessons = [lesson for unit in course["units"] for lesson in unit["lessons"]]
    assert [lesson["generation_status"] for lesson in lessons] == [
        "ready",
        "not_started",
        "not_started",
    ]
    assert lessons[0]["question_count"] == 6 and lessons[0]["flagged_count"] == 1
    with migrated_engine.connect() as conn:
        chunks = conn.execute(
            text("SELECT count(*), max(vector_dims(embedding)) FROM note_chunks")
        ).one()
        assert chunks[0] >= 2 and chunks[1] == 1024
        versions = (
            conn.execute(text("SELECT DISTINCT prompt_version FROM questions")).scalars().all()
        )
        assert versions == ["questions-v1"]
        terms = conn.execute(text("SELECT count(*) FROM terms")).scalar()
        assert terms == 5


def test_lesson_two_waits_until_lesson_one_is_opened(
    people: Any, api: Any, services: Services
) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    llm = services.llm
    assert isinstance(llm, FakeLLMProvider)

    def question_calls() -> int:
        return sum("practice questions" in r.prompt for r in llm.requests)

    assert question_calls() == 1  # only lesson 1 so far

    lesson = teacher.get(f"/lessons/{status['first_lesson_id']}").json()
    assert lesson["generation_status"] == "ready"
    assert question_calls() == 2  # opening lesson 1 generated lesson 2
    teacher.get(f"/lessons/{status['first_lesson_id']}")
    assert question_calls() == 2  # ...exactly once

    course = teacher.get(f"/courses/{status['course_id']}").json()
    lessons = [lesson for unit in course["units"] for lesson in unit["lessons"]]
    assert [lesson["generation_status"] for lesson in lessons] == ["ready", "ready", "not_started"]


def test_flagged_question_carries_reason(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    questions = teacher.get(f"/lessons/{status['first_lesson_id']}").json()["questions"]
    flagged = [q for q in questions if q["flag_reason"]]
    assert len(flagged) == 1
    assert "page 2" in flagged[0]["flag_reason"]
    assert flagged[0]["confidence"] < 0.6


def test_invalid_json_retries_once_then_needs_review(
    services: Services, people: Any, api: Any
) -> None:
    def broken_questions(request: LLMRequest) -> str:
        if "practice questions" in request.prompt:
            return '{"questions": "not a list"}'
        return fake_llm_response(request)

    services.llm = FakeLLMProvider(responder=broken_questions)
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    assert status["status"] == "ready"  # the course exists; lesson 1 is marked for review
    llm = services.llm
    assert isinstance(llm, FakeLLMProvider)
    assert sum("practice questions" in r.prompt for r in llm.requests) == 2  # one retry
    course = teacher.get(f"/courses/{status['course_id']}").json()
    first = course["units"][0]["lessons"][0]
    assert first["generation_status"] == "needs_review"


def test_invalid_outline_marks_upload_needs_review(
    services: Services, people: Any, api: Any
) -> None:
    services.llm = FakeLLMProvider(responder=lambda r: "not json at all")
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    assert status["status"] == "needs_review"
    assert status["course_id"] is None


def test_invented_page_numbers_are_corrected_and_flagged(
    services: Services, people: Any, api: Any
) -> None:
    def page_99(request: LLMRequest) -> str:
        raw = fake_llm_response(request)
        if "practice questions" in request.prompt:
            data = json.loads(raw)
            data["questions"][0]["source_page"] = 99
            return json.dumps(data)
        return raw

    services.llm = FakeLLMProvider(responder=page_99)
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    q0 = teacher.get(f"/lessons/{status['first_lesson_id']}").json()["questions"][0]
    assert q0["source_page"] in (1, 2)
    assert "page 99" in q0["flag_reason"]


# ---- limits and permissions ------------------------------------------------------------------


def test_upload_limits(people: Any) -> None:
    teacher, _ = teacher_with_class(people)
    too_many = teacher.post("/uploads", {"pages": [{"kind": "text", "text": "x"}] * 11})
    assert too_many.status_code == 422
    big_pdf = teacher.post("/uploads", {"pages": [{"kind": "pdf", "size_bytes": 11 * 1024 * 1024}]})
    assert big_pdf.status_code == 413
    assert big_pdf.json()["detail"]["code"] == "FILE_TOO_LARGE"


def test_start_refuses_missing_photos(people: Any) -> None:
    teacher, _ = teacher_with_class(people)
    upload = teacher.post("/uploads", {"pages": [{"kind": "photo", "size_bytes": 10}]}).json()
    r = teacher.post(f"/uploads/{upload['id']}/start", {})
    assert r.status_code == 409 and r.json()["detail"]["code"] == "PAGES_MISSING"


def test_other_teacher_cannot_see_course(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    other = people("Mr. Shah", ["teacher"])
    assert other.get(f"/courses/{status['course_id']}").status_code == 404
    assert other.get(f"/lessons/{status['first_lesson_id']}").status_code == 404
    assert other.get(f"/uploads/{status['id']}").status_code == 404


def test_student_gets_403_for_unpublished_lesson(
    people: Any, api: Any, migrated_engine: Engine
) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    student = people("Ananya", ["student"])
    assert student.post("/classrooms/join", {"key": classroom["join_key"]}).status_code == 201

    # Not assigned yet: the course doesn't exist for the student.
    assert student.get(f"/courses/{status['course_id']}").status_code == 404
    assert student.get("/me/courses").json() == []

    first = status["first_lesson_id"]
    published = teacher.post(f"/lessons/{first}/publish", {"classroom_id": classroom["id"]})
    assert published.status_code == 200, published.text

    # Lesson 1 is published; lesson 2 isn't, so the student's course view doesn't list it.
    course = student.get(f"/courses/{status['course_id']}").json()
    assert [lesson["id"] for unit in course["units"] for lesson in unit["lessons"]] == [first]
    teacher_view = teacher.get(f"/courses/{status['course_id']}").json()
    second = teacher_view["units"][0]["lessons"][1]["id"]
    assert student.get(f"/lessons/{first}").status_code == 200
    r = student.get(f"/lessons/{second}")
    assert r.status_code == 403
    assert r.json() == {"detail": {"code": "LESSON_NOT_PUBLISHED"}}
    mine = student.get("/me/courses").json()
    assert [c["id"] for c in mine] == [status["course_id"]] and mine[0]["published_count"] == 1

    # Row-level security agrees: as the student, unpublished lessons and their questions are hidden.
    with migrated_engine.connect() as conn, conn.begin():
        session = Session(bind=conn)
        act_as(session, student.id)
        visible = session.execute(text("SELECT id FROM lessons")).scalars().all()
        assert {str(v) for v in visible} == {first}
        hidden_questions = session.execute(
            text("SELECT count(*) FROM questions WHERE lesson_id = :l"), {"l": second}
        ).scalar()
        assert hidden_questions == 0


def test_student_outside_class_sees_nothing(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    teacher.post(f"/lessons/{status['first_lesson_id']}/publish", {"classroom_id": classroom["id"]})
    outsider = people("Ravi", ["student"])
    assert outsider.get(f"/lessons/{status['first_lesson_id']}").status_code == 404
    assert outsider.get("/me/courses").json() == []


# ---- review: edit, regenerate, delete, assign ----------------------------------------------


def test_teacher_edits_regenerates_and_deletes(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    questions = teacher.get(f"/lessons/{status['first_lesson_id']}").json()["questions"]
    flagged = next(q for q in questions if q["flag_reason"])

    edited = {
        "type": "true_false",
        "prompt": {"en": "True or false?", "hi": "सही या गलत?", "kn": "ಸರಿ ಅಥವಾ ತಪ್ಪು?"},
        "statement": {
            "en": "Plants make food.",
            "hi": "पौधे भोजन बनाते हैं।",
            "kn": "ಸಸ್ಯಗಳು ಆಹಾರ ಮಾಡುತ್ತವೆ.",
        },
        "answer": True,
        "explanation": {"en": "Using sunlight.", "hi": "धूप से।", "kn": "ಸೂರ್ಯನ ಬೆಳಕಿನಿಂದ."},
    }
    r = teacher.put(f"/questions/{flagged['id']}", {"question": edited})
    assert r.status_code == 200, r.text
    assert r.json()["teacher_edited"] is True and r.json()["flag_reason"] is None
    assert r.json()["type"] == "true_false"

    bad = teacher.put(f"/questions/{flagged['id']}", {"question": {"type": "true_false"}})
    assert bad.status_code == 422

    regen = teacher.post(f"/questions/{questions[0]['id']}/regenerate", {})
    assert regen.status_code == 200, regen.text
    assert regen.json()["id"] == questions[0]["id"] and regen.json()["teacher_edited"] is False

    assert teacher.delete(f"/questions/{questions[1]['id']}").status_code == 204
    after = teacher.get(f"/lessons/{status['first_lesson_id']}").json()["questions"]
    assert len(after) == len(questions) - 1

    course = teacher.get(f"/courses/{status['course_id']}").json()
    assert course["edited_count"] == 1


def test_student_cannot_edit_questions(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    q = teacher.get(f"/lessons/{status['first_lesson_id']}").json()["questions"][0]
    student = people("Ananya", ["student"])
    assert student.delete(f"/questions/{q['id']}").status_code == 404


def test_assign_course_with_due_date_and_edit_title(people: Any, api: Any) -> None:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    r = teacher.post(
        f"/courses/{status['course_id']}/assign",
        {"classroom_ids": [classroom["id"]], "due_at": "2026-12-01T10:00:00Z"},
    )
    assert r.status_code == 200, r.text
    assert r.json()["assigned_classroom_ids"] == [classroom["id"]]
    renamed = teacher.put(f"/courses/{status['course_id']}", {"title": {"en": "Cells!"}})
    assert renamed.json()["title"]["en"] == "Cells!" and renamed.json()["title"]["hi"] == "कोशिका"
    assert [c["id"] for c in teacher.get("/courses").json()] == [status["course_id"]]


def test_student_makes_personal_ai_made_course(people: Any, api: Any) -> None:
    student = people("Ananya", ["student"])
    status = make_course(api, student, personal=True)
    assert status["status"] == "ready"
    mine = student.get("/me/courses").json()
    assert len(mine) == 1 and mine[0]["ai_made"] is True and mine[0]["status"] == "published"
    assert student.get(f"/lessons/{status['first_lesson_id']}").status_code == 200
