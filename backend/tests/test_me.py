import time

import pytest
from fastapi.testclient import TestClient

from tests.conftest import TEST_USER_ID, make_token


def test_me_rejects_missing_token(client: TestClient) -> None:
    response = client.get("/me")
    assert response.status_code == 401


def test_me_accepts_valid_token(api: TestClient) -> None:
    response = api.get("/me", headers={"Authorization": f"Bearer {make_token()}"})
    assert response.status_code == 200
    assert response.json()["id"] == TEST_USER_ID
    assert response.json()["registered"] is False


@pytest.mark.parametrize(
    "token",
    [
        "not-a-jwt",
        make_token(secret="wrong-secret-also-32-bytes-long-0000000"),
        make_token(exp=int(time.time()) - 10),
        make_token(aud="anon"),
    ],
    ids=["garbage", "wrong-secret", "expired", "wrong-audience"],
)
def test_me_rejects_invalid_token(client: TestClient, token: str) -> None:
    response = client.get("/me", headers={"Authorization": f"Bearer {token}"})
    assert response.status_code == 401
    assert response.json() == {"detail": {"code": "UNAUTHORIZED"}}


def test_me_rejects_non_bearer_scheme(client: TestClient) -> None:
    response = client.get("/me", headers={"Authorization": f"Basic {make_token()}"})
    assert response.status_code == 401
