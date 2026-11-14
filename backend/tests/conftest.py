import time
from collections.abc import Iterator
from typing import Any

import jwt
import pytest
from fastapi.testclient import TestClient

from app.config import Settings, get_settings
from app.main import create_app

TEST_SECRET = "test-secret-at-least-32-bytes-long-000000"
TEST_USER_ID = "8f14e45f-ceea-467a-9575-6a1f3c2b7d10"


@pytest.fixture
def settings() -> Settings:
    return Settings(supabase_jwt_secret=TEST_SECRET, supabase_url="", _env_file=None)


@pytest.fixture
def client(settings: Settings) -> Iterator[TestClient]:
    app = create_app()
    app.dependency_overrides[get_settings] = lambda: settings
    with TestClient(app) as c:
        yield c


def make_token(secret: str = TEST_SECRET, **overrides: Any) -> str:
    claims: dict[str, Any] = {
        "sub": TEST_USER_ID,
        "aud": "authenticated",
        "role": "authenticated",
        "exp": int(time.time()) + 3600,
    }
    claims.update(overrides)
    return jwt.encode(claims, secret, algorithm="HS256")
