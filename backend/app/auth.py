"""Supabase access-token verification.

Supabase signs access tokens either with the legacy shared secret (HS256) or with
asymmetric signing keys (ES256/RS256) published at `<SUPABASE_URL>/auth/v1/.well-known/jwks.json`.
Both are accepted here: HS256 tokens are checked with SUPABASE_JWT_SECRET, asymmetric ones
against the project's JWKS. See docs/decisions.md.
"""

from dataclasses import dataclass
from functools import lru_cache
from typing import Annotated, Any

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from app.config import Settings, get_settings

_bearer = HTTPBearer(auto_error=False)
_ASYMMETRIC = {"ES256", "RS256"}


@dataclass(frozen=True)
class CurrentUser:
    id: str
    claims: dict[str, Any]


class InvalidTokenError(Exception):
    pass


@lru_cache(maxsize=4)
def _jwks_client(supabase_url: str) -> jwt.PyJWKClient:
    # PyJWKClient caches keys; lifespan kept under Supabase's 10-minute edge cache.
    return jwt.PyJWKClient(
        f"{supabase_url.rstrip('/')}/auth/v1/.well-known/jwks.json", lifespan=600
    )


def verify_token(token: str, settings: Settings) -> dict[str, Any]:
    try:
        header = jwt.get_unverified_header(token)
        alg = header.get("alg")
        if alg == "HS256":
            if not settings.supabase_jwt_secret:
                raise InvalidTokenError("HS256 token but no SUPABASE_JWT_SECRET configured")
            key: Any = settings.supabase_jwt_secret
        elif alg in _ASYMMETRIC:
            if not settings.supabase_url:
                raise InvalidTokenError("asymmetric token but no SUPABASE_URL configured")
            key = _jwks_client(settings.supabase_url).get_signing_key_from_jwt(token).key
        else:
            raise InvalidTokenError(f"unsupported alg {alg!r}")
        claims: dict[str, Any] = jwt.decode(
            token,
            key,
            algorithms=[alg],
            audience="authenticated",
            options={"require": ["exp", "sub"]},
        )
    except jwt.PyJWTError as exc:
        raise InvalidTokenError(str(exc)) from exc
    return claims


def get_current_user(
    credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(_bearer)],
    settings: Annotated[Settings, Depends(get_settings)],
) -> CurrentUser:
    unauthorized = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail={"code": "UNAUTHORIZED"},
        headers={"WWW-Authenticate": "Bearer"},
    )
    if credentials is None or credentials.scheme.lower() != "bearer":
        raise unauthorized
    try:
        claims = verify_token(credentials.credentials, settings)
    except InvalidTokenError as exc:
        raise unauthorized from exc
    return CurrentUser(id=str(claims["sub"]), claims=claims)
