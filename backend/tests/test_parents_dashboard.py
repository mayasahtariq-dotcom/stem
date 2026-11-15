"""M4 against Postgres: the consent gate, parent links and consent, the parent dashboard,
deletion, the teacher dashboard (checked by hand) and the Student Twin."""

from datetime import UTC, datetime, timedelta
from typing import Any

import pytest
from sqlalchemy import Engine, text
from sqlalchemy.orm import Session

from app.db.session import act_as
from app.routers import dashboard, parents
from app.twin import service as twin
from tests.test_courses import make_course, teacher_with_class


def setup_class(api: Any, people: Any, school_consent: bool = False) -> dict[str, Any]:
    teacher, classroom = teacher_with_class(people)
    status = make_course(api, teacher, classroom["id"])
    r = teacher.post(
        f"/lessons/{status['first_lesson_id']}/publish", {"classroom_id": classroom["id"]}
    )
    assert r.status_code == 200, r.text
    student = people("Asha", ["student"])
    assert student.post("/classrooms/join", {"key": classroom["join_key"]}).status_code == 201
    if school_consent:
        teacher.put(f"/classrooms/{classroom['id']}/school-consent", {"enabled": True})
    return {"teacher": teacher, "classroom": classroom, "status": status, "student": student}


def link_parent(people: Any, student: Any, name: str = "Mr. Rao") -> tuple[Any, dict[str, Any]]:
    parent = people(name, ["parent"])
    code = student.post("/me/link-code", {}).json()["code"]
    child = parent.post("/parent/link", {"code": code})
    assert child.status_code == 201, child.text
    return parent, child.json()


def answer(student: Any, q: dict[str, Any], correct: bool) -> Any:
    c = q["content"]
    if q["type"] == "multiple_choice":
        i = c["correct_index"] if correct else (c["correct_index"] + 1) % len(c["options"])
        body: dict[str, Any] = {"index": i}
    elif q["type"] == "true_false":
        body = {"value": c["answer"] if correct else not c["answer"]}
    else:
        raise AssertionError(q["type"])
    return student.post("/attempts", {"question_id": q["id"], "answer": body})


# ---- consent ---------------------------------------------------------------------------------


def test_pending_student_needs_parent_or_school_consent(api: Any, people: Any) -> None:
    s = setup_class(api, people)
    student, lesson = s["student"], s["status"]["first_lesson_id"]
    blocked = student.post(f"/lessons/{lesson}/start", {})
    assert blocked.status_code == 403 and blocked.json()["detail"]["code"] == "CONSENT_REQUIRED"

    parent, child = link_parent(people, student)
    assert child["consent_status"] == "pending"
    assert student.post(f"/lessons/{lesson}/start", {}).status_code == 403
    approved = parent.post(f"/parent/children/{child['id']}/consent", {})
    assert approved.json()["consent_status"] == "approved"
    assert student.post(f"/lessons/{lesson}/start", {}).status_code == 200

    # Revoking pauses the account.
    parent.post(f"/parent/children/{child['id']}/revoke-consent", {})
    assert student.post(f"/lessons/{lesson}/start", {}).status_code == 403


def test_school_consent_skips_the_parent_step(api: Any, people: Any) -> None:
    s = setup_class(api, people, school_consent=True)
    assert (
        s["student"].post(f"/lessons/{s['status']['first_lesson_id']}/start", {}).status_code == 200
    )


# ---- links -----------------------------------------------------------------------------------


def test_link_codes_work_once_and_expire(
    api: Any, people: Any, monkeypatch: pytest.MonkeyPatch
) -> None:
    s = setup_class(api, people)
    student = s["student"]
    parent = people("Mr. Rao", ["parent"])
    code = student.post("/me/link-code", {}).json()["code"]
    assert len(code) == 6
    preview = parent.get(f"/parent/link/{code}").json()
    assert preview["name"] == "Asha" and preview["classes"][0]["teacher_name"] == "Mrs. Rao"
    assert parent.post("/parent/link", {"code": code}).status_code == 201
    other = people("Other", ["parent"])
    reused = other.post("/parent/link", {"code": code})
    assert reused.status_code == 410 and reused.json()["detail"]["code"] == "CODE_EXPIRED"

    late = student.post("/me/link-code", {}).json()["code"]
    real_now = parents.now()
    monkeypatch.setattr(parents, "now", lambda: real_now + timedelta(hours=24, minutes=1))
    assert other.post("/parent/link", {"code": late}).status_code == 410
    assert (
        other.post("/parent/link", {"code": "ZZZZZZ"}).json()["detail"]["code"] == "CODE_NOT_FOUND"
    )


def test_only_parents_link_and_at_most_two(api: Any, people: Any) -> None:
    s = setup_class(api, people)
    student = s["student"]
    code = student.post("/me/link-code", {}).json()["code"]
    assert (
        s["teacher"].post("/parent/link", {"code": code}).json()["detail"]["code"] == "NOT_A_PARENT"
    )
    link_parent(people, student, "Mother")
    link_parent(people, student, "Father")
    third = people("Uncle", ["parent"])
    code = student.post("/me/link-code", {}).json()["code"]
    assert third.post("/parent/link", {"code": code}).json()["detail"]["code"] == "TOO_MANY_PARENTS"
    mine = student.get("/me/parents").json()
    assert [p["name"] for p in mine] == ["Mother", "Father"]
    assert all(len(p["phone_last4"]) == 4 for p in mine)


def test_parent_loses_access_after_the_link_ends(api: Any, people: Any) -> None:
    s = setup_class(api, people)
    parent, child = link_parent(people, s["student"])
    assert parent.get(f"/parent/children/{child['id']}/dashboard").status_code == 200
    removed = s["student"].delete(f"/me/parents/{s['student'].get('/me/parents').json()[0]['id']}")
    assert removed.status_code == 204
    assert parent.get(f"/parent/children/{child['id']}/dashboard").status_code == 404
    assert parent.get("/parent/children").json() == []


def test_parents_never_see_doubts_or_other_students(
    api: Any, people: Any, migrated_engine: Engine
) -> None:
    s = setup_class(api, people, school_consent=True)
    student = s["student"]
    student.post(
        "/doubts", {"course_id": s["status"]["course_id"], "text": "What do mitochondria do?"}
    )
    parent, child = link_parent(people, student)
    stranger = people("Ravi", ["student"])
    with Session(migrated_engine) as db, db.begin():
        act_as(db, parent.id)
        assert db.execute(text("SELECT count(*) FROM doubts")).scalar() == 0
        visible = db.execute(text("SELECT id::text FROM users WHERE id <> :me"), {"me": parent.id})
        assert {r[0] for r in visible} == {child["id"]}  # the child only, not other students
    assert parent.get(f"/parent/children/{stranger.id}/dashboard").status_code == 404
    dash = parent.get(f"/parent/children/{child['id']}/dashboard").json()
    assert "doubt" not in str(dash).lower()


# ---- parent dashboard and deletion -----------------------------------------------------------


def test_parent_dashboard_empty_then_active(api: Any, people: Any) -> None:
    s = setup_class(api, people, school_consent=True)
    student = s["student"]
    parent, child = link_parent(people, student)
    empty = parent.get(f"/parent/children/{child['id']}/dashboard").json()
    assert empty["has_activity"] is False
    assert empty["week"]["arrow"] == "same" and empty["week"]["lessons_this_week"] == 0
    assert empty["assignments"][0]["status"] == "pending"
    assert "hasn't finished a lesson" in empty["summary"]["en"]
    assert empty["summary"]["hi"] and empty["summary"]["kn"]

    lesson = s["status"]["first_lesson_id"]
    for q in student.post(f"/lessons/{lesson}/start", {}).json()["lesson"]["questions"]:
        answer(student, q, correct=True)
    student.post(f"/lessons/{lesson}/complete", {})
    dash = parent.get(f"/parent/children/{child['id']}/dashboard").json()
    assert dash["has_activity"] is True
    assert dash["week"]["arrow"] == "up" and dash["week"]["lessons_this_week"] == 1
    assert dash["week"]["streak"] == 1
    assert dash["accuracy"]["this_week"] == 1.0
    assert dash["assignments"][0]["status"] == "done"
    assert dash["subjects"][0]["subject"] == "BI" and dash["subjects"][0]["strongest"]
    assert dash["classes"][0]["teacher_name"] == "Mrs. Rao"


def test_deletion_removes_the_child_and_their_twin(
    api: Any, people: Any, migrated_engine: Engine
) -> None:
    s = setup_class(api, people, school_consent=True)
    student = s["student"]
    q = student.post(f"/lessons/{s['status']['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ][0]
    answer(student, q, correct=False)
    parent, child = link_parent(people, student)
    with migrated_engine.connect() as c:
        assert (c.execute(text("SELECT count(*) FROM twin_concepts")).scalar() or 0) > 0
    r = parent.post(f"/parent/children/{child['id']}/deletion", {})
    assert r.status_code == 202, r.text
    with migrated_engine.connect() as c:
        assert (
            c.execute(
                text("SELECT count(*) FROM users WHERE id = :id"), {"id": child["id"]}
            ).scalar()
            == 0
        )
        assert c.execute(text("SELECT count(*) FROM twin_concepts")).scalar() == 0
        assert c.execute(text("SELECT count(*) FROM answer_attempts")).scalar() == 0
        assert (
            c.execute(text("SELECT completed_at IS NOT NULL FROM deletion_requests")).scalar()
            is True
        )


# ---- teacher dashboard -----------------------------------------------------------------------


def test_teacher_dashboard_matches_a_hand_calculation(
    api: Any, people: Any, monkeypatch: pytest.MonkeyPatch
) -> None:
    s = setup_class(api, people, school_consent=True)
    teacher, classroom, asha = s["teacher"], s["classroom"], s["student"]
    ravi = people("Ravi", ["student"])
    ravi.post("/classrooms/join", {"key": classroom["join_key"]})
    lesson = s["status"]["first_lesson_id"]
    qs = asha.post(f"/lessons/{lesson}/start", {}).json()["lesson"]["questions"]
    # Asha: 3 first tries, 2 right (accuracy 2/3), then finishes the lesson. Ravi does nothing.
    answer(asha, qs[0], True)
    answer(asha, qs[1], True)
    answer(asha, qs[2], False)
    asha.post(f"/lessons/{lesson}/complete", {})

    d = teacher.get(f"/classrooms/{classroom['id']}/dashboard").json()
    assert d["tiles"] == {"active_this_week": 1, "student_count": 2, "accuracy": 0.667, "behind": 0}
    lesson_topic = next(w for w in d["weak_topics"] if w["kind"] == "lesson")
    assert lesson_topic["accuracy"] == 0.667 and lesson_topic["attempts"] == 3
    assert [r["name"] for r in d["students"]] == ["Asha", "Ravi"]  # nobody behind: by name
    assert d["students"][0]["accuracy"] == pytest.approx(2 / 3)

    # Four days later both are idle (over three days): behind, listed first, in red.
    later = datetime.now(UTC) + timedelta(days=4)
    monkeypatch.setattr(dashboard, "now", lambda: later)
    d2 = teacher.get(f"/classrooms/{classroom['id']}/dashboard").json()
    assert d2["tiles"]["behind"] == 2 and d2["tiles"]["active_this_week"] == 1
    assert all(r["idle"] and r["idle_days"] >= 4 for r in d2["students"])


def test_student_detail_has_twin_insights_but_no_doubts(api: Any, people: Any) -> None:
    s = setup_class(api, people, school_consent=True)
    teacher, asha = s["teacher"], s["student"]
    qs = asha.post(f"/lessons/{s['status']['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ]
    answer(asha, qs[0], False)  # picks a wrong term: a confusion
    asha.post("/doubts", {"course_id": s["status"]["course_id"], "text": "Secret question"})
    detail = teacher.get(f"/classrooms/{s['classroom']['id']}/students/{asha.id}").json()
    assert detail["weakest"] and detail["confusions"][0]["count"] == 1
    assert "Secret question" not in str(detail)
    assert detail["lessons"][0]["accuracy"] == 0.0


def test_teachers_only_see_their_own_classes(api: Any, people: Any) -> None:
    s = setup_class(api, people)
    other = people("Mr. Iyer", ["teacher"])
    cid = s["classroom"]["id"]
    assert other.get(f"/classrooms/{cid}/dashboard").status_code == 404
    assert other.get(f"/classrooms/{cid}/students/{s['student'].id}").status_code == 404
    assert other.put(f"/classrooms/{cid}/school-consent", {"enabled": True}).status_code == 404


# ---- the twin ------------------------------------------------------------------------------


def test_twin_rebuild_matches_live_updates(api: Any, people: Any, migrated_engine: Engine) -> None:
    s = setup_class(api, people, school_consent=True)
    asha = s["student"]
    qs = asha.post(f"/lessons/{s['status']['first_lesson_id']}/start", {}).json()["lesson"][
        "questions"
    ]
    for i, q in enumerate(qs):
        if q["type"] in ("multiple_choice", "true_false"):
            answer(asha, q, correct=i % 2 == 0)

    query = text(
        "SELECT kind, key, round(mastery::numeric, 4), attempts FROM twin_concepts"
        " ORDER BY kind, key"
    )
    with migrated_engine.connect() as c:
        live = c.execute(query).all()
    assert any(k == "term" for k, *_ in live) and any(k == "lesson" for k, *_ in live)
    with Session(migrated_engine) as db, db.begin():
        import uuid

        replayed = twin.rebuild(db, uuid.UUID(asha.id))
    assert replayed == sum(1 for q in qs if q["type"] in ("multiple_choice", "true_false"))
    with migrated_engine.connect() as c:
        assert c.execute(query).all() == live
