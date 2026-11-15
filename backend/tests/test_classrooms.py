"""Classroom endpoints, join errors, rate limiting and row-level security (needs Postgres)."""

from datetime import UTC, datetime, timedelta
from typing import Any

from sqlalchemy import Engine, text

from app.db.session import act_as

CLASS = {"name": "Class 9-A Science", "subject": "BI", "grade": 9}


def create_class(teacher: Any, **overrides: Any) -> dict[str, Any]:
    r = teacher.post("/classrooms", {**CLASS, **overrides})
    assert r.status_code == 201, r.text
    body: dict[str, Any] = r.json()
    return body


# ---- profile ------------------------------------------------------------------------------


def test_me_before_and_after_profile(people: Any, api: Any, migrated_engine: Engine) -> None:
    from tests.conftest import Person

    p = Person(api, migrated_engine, "+919000000001")
    first = p.get("/me").json()
    assert first == {
        "id": p.id,
        "registered": False,
        "name": None,
        "phone": None,
        "roles": [],
        "language_mode": None,
    }
    p.setup("Ananya", ["student"], "kannada")
    me = p.get("/me").json()
    assert me["registered"] is True
    assert me["name"] == "Ananya"
    assert me["phone"] == "+919000000001"
    assert me["roles"] == ["student"]
    assert me["language_mode"] == "kannada"


def test_put_me_updates_roles_and_language(people: Any) -> None:
    p = people("Ravi", ["student"])
    r = p.api.put(
        "/me",
        json={
            "name": " Ravi K ",
            "roles": ["student", "parent", "student"],
            "language_mode": "hindi",
        },
        headers=p.headers,
    )
    assert r.status_code == 200
    assert r.json()["name"] == "Ravi K"
    assert r.json()["roles"] == ["student", "parent"]


def test_put_me_rejects_empty_roles(people: Any) -> None:
    p = people("Ravi", ["student"])
    r = p.api.put(
        "/me", json={"name": "Ravi", "roles": [], "language_mode": "hindi"}, headers=p.headers
    )
    assert r.status_code == 422


# ---- teachers -------------------------------------------------------------------------------


def test_teacher_creates_and_lists_classrooms(people: Any) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    created = create_class(teacher)
    assert created["join_key"].startswith("BI09-")
    assert created["student_count"] == 0
    assert created["allowed_language_modes"] == ["hindi", "kannada"]

    listed = teacher.get("/classrooms").json()
    assert [c["id"] for c in listed] == [created["id"]]


def test_non_teacher_cannot_create(people: Any) -> None:
    student = people("Ananya", ["student"])
    r = student.post("/classrooms", CLASS)
    assert r.status_code == 403
    assert r.json() == {"detail": {"code": "NOT_A_TEACHER"}}


def test_create_without_profile_is_refused(api: Any, migrated_engine: Engine) -> None:
    from tests.conftest import Person

    p = Person(api, migrated_engine, "+919000000002")
    r = p.post("/classrooms", CLASS)
    assert r.status_code == 409
    assert r.json()["detail"]["code"] == "PROFILE_INCOMPLETE"


def test_teacher_sees_members_of_own_classroom(people: Any) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    a = people("Ananya", ["student"])
    b = people("Ravi", ["student"])
    for s in (a, b):
        assert s.post("/classrooms/join", {"key": c["join_key"]}).status_code == 201

    detail = teacher.get(f"/classrooms/{c['id']}").json()
    assert [m["name"] for m in detail["members"]] == ["Ananya", "Ravi"]
    assert detail["student_count"] == 2
    assert teacher.get("/classrooms").json()[0]["student_count"] == 2


def test_other_teacher_cannot_open_classroom(people: Any) -> None:
    rao = people("Mrs. Rao", ["teacher"])
    c = create_class(rao)
    other = people("Mr. Shah", ["teacher"])
    assert other.get(f"/classrooms/{c['id']}").status_code == 404
    assert other.get("/classrooms").json() == []


# ---- students: joining ----------------------------------------------------------------------


def test_preview_then_join_then_my_classrooms(people: Any) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    student = people("Ananya", ["student"])

    preview = student.get(f"/classrooms/preview/{c['join_key'].lower().replace('-', '')}")
    assert preview.status_code == 200, preview.text
    assert preview.json() == {
        "id": c["id"],
        "name": "Class 9-A Science",
        "teacher_name": "Mrs. Rao",
        "subject": "BI",
        "grade": 9,
        "student_count": 0,
        "allowed_language_modes": ["hindi", "kannada"],
    }

    joined = student.post("/classrooms/join", {"key": c["join_key"]})
    assert joined.status_code == 201
    assert joined.json()["teacher_name"] == "Mrs. Rao"
    assert joined.json()["student_count"] == 1

    mine = student.get("/me/classrooms").json()
    assert [m["id"] for m in mine] == [c["id"]]


def test_join_errors(people: Any, migrated_engine: Engine) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    kannada_only = create_class(teacher, allowed_language_modes=["kannada"])
    expired = create_class(teacher, name="Old class")
    with migrated_engine.begin() as conn:
        conn.execute(
            text("UPDATE classrooms SET expires_at = :t WHERE id = :id"),
            {"t": datetime.now(UTC) - timedelta(days=1), "id": expired["id"]},
        )
    student = people("Ananya", ["student"], "hindi")

    def code(key: str) -> str:
        r = student.post("/classrooms/join", {"key": key})
        assert r.status_code >= 400, r.text
        result: str = r.json()["detail"]["code"]
        return result

    assert code("BI09-ZZZZ") == "KEY_NOT_FOUND"
    assert code("not-a-key!") == "KEY_NOT_FOUND"
    assert code(expired["join_key"]) == "KEY_EXPIRED"
    assert code(kannada_only["join_key"]) == "LANGUAGE_NOT_ALLOWED"
    assert student.post("/classrooms/join", {"key": c["join_key"]}).status_code == 201
    assert code(c["join_key"]) == "ALREADY_MEMBER"


def test_teacher_only_account_cannot_join(people: Any) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    r = teacher.post("/classrooms/join", {"key": c["join_key"]})
    assert r.status_code == 403
    assert r.json()["detail"]["code"] == "NOT_A_STUDENT"


def test_eleventh_attempt_in_an_hour_is_refused(people: Any) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    student = people("Ananya", ["student"])
    for i in range(10):
        r = student.get(f"/classrooms/preview/BI09-ZZZ{'ABCDEFGHJK'[i]}")
        assert r.status_code == 404, (i, r.text)
    eleventh = student.post("/classrooms/join", {"key": c["join_key"]})
    assert eleventh.status_code == 429
    assert eleventh.json() == {"detail": {"code": "TOO_MANY_ATTEMPTS"}}


def test_old_attempts_do_not_count(people: Any, migrated_engine: Engine) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    c = create_class(teacher)
    student = people("Ananya", ["student"])
    for _ in range(10):
        student.get("/classrooms/preview/BI09-ZZZZ")
    with migrated_engine.begin() as conn:
        conn.execute(text("UPDATE join_attempts SET created_at = now() - interval '61 minutes'"))
    assert student.post("/classrooms/join", {"key": c["join_key"]}).status_code == 201


# ---- row-level security ---------------------------------------------------------------------


def test_student_cannot_see_another_classrooms_members(
    people: Any, migrated_engine: Engine
) -> None:
    teacher = people("Mrs. Rao", ["teacher"])
    class_a = create_class(teacher, name="9-A")
    class_b = create_class(teacher, name="9-B")
    ananya = people("Ananya", ["student"])
    ravi = people("Ravi", ["student"])
    priya = people("Priya", ["student"])
    assert ananya.post("/classrooms/join", {"key": class_a["join_key"]}).status_code == 201
    assert priya.post("/classrooms/join", {"key": class_a["join_key"]}).status_code == 201
    assert ravi.post("/classrooms/join", {"key": class_b["join_key"]}).status_code == 201

    # Through the API: a student can't open member lists at all.
    assert ananya.get(f"/classrooms/{class_b['id']}").status_code == 404
    assert ananya.get(f"/classrooms/{class_a['id']}").status_code == 404

    # Directly against the database as Ananya: RLS hides other memberships and classrooms.
    with migrated_engine.connect() as conn, conn.begin():
        from sqlalchemy.orm import Session

        session = Session(bind=conn)
        act_as(session, ananya.id)
        rows = session.execute(text("SELECT classroom_id, student_id FROM memberships")).all()
        assert {(str(r[0]), str(r[1])) for r in rows} == {(class_a["id"], ananya.id)}
        visible = session.execute(text("SELECT id FROM classrooms")).scalars().all()
        assert {str(v) for v in visible} == {class_a["id"]}
        names = session.execute(text("SELECT name FROM users ORDER BY name")).scalars().all()
        assert names == ["Ananya", "Mrs. Rao"]  # herself and her teacher only


def test_teacher_rls_sees_only_own_classrooms(people: Any, migrated_engine: Engine) -> None:
    rao = people("Mrs. Rao", ["teacher"])
    shah = people("Mr. Shah", ["teacher"])
    mine = create_class(rao)
    create_class(shah, subject="PH", grade=8)
    with migrated_engine.connect() as conn, conn.begin():
        from sqlalchemy.orm import Session

        session = Session(bind=conn)
        act_as(session, rao.id)
        visible = session.execute(text("SELECT id FROM classrooms")).scalars().all()
        assert {str(v) for v in visible} == {mine["id"]}


def test_phone_used_by_another_account_is_a_clear_error(api: Any, migrated_engine: Engine) -> None:
    from tests.conftest import Person

    first = Person(api, migrated_engine, "+919000000077").setup("First", ["student"])
    assert first.get("/me").json()["phone"] == "+919000000077"
    second = Person(api, migrated_engine, "+919000000077")
    r = second.api.put(
        "/me",
        json={"name": "Second", "roles": ["student"], "language_mode": "hindi"},
        headers=second.headers,
    )
    assert r.status_code == 409
    assert r.json() == {"detail": {"code": "PHONE_IN_USE"}}
