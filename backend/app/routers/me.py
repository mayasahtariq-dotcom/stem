import uuid
from typing import Annotated, Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import Engine, select, text
from sqlalchemy.dialects.postgresql import insert
from sqlalchemy.exc import IntegrityError

from app.auth import CurrentUser, get_current_user
from app.config import Settings, get_settings
from app.db.models import User
from app.db.session import UserDb, get_engine
from app.errors import ErrorCode, api_error

router = APIRouter(tags=["me"])

Role = Literal["student", "teacher", "parent"]
LanguageMode = Literal["hindi", "kannada"]


class MeResponse(BaseModel):
    id: str
    registered: bool = False
    name: str | None = None
    phone: str | None = None
    roles: list[Role] = []
    language_mode: LanguageMode | None = None


class ProfileUpdate(BaseModel):
    name: str = Field(min_length=1, max_length=60)
    language_mode: LanguageMode
    roles: list[Role] = Field(min_length=1)

    @field_validator("name")
    @classmethod
    def strip_name(cls, v: str) -> str:
        v = v.strip()
        if not v:
            raise ValueError("name is blank")
        return v

    @field_validator("roles")
    @classmethod
    def dedupe_roles(cls, v: list[Role]) -> list[Role]:
        return list(dict.fromkeys(v))


def _to_response(user: User) -> MeResponse:
    return MeResponse(
        id=str(user.id),
        registered=True,
        name=user.name,
        phone=user.phone,
        roles=user.roles,
        language_mode=user.language_mode,
    )


def _phone_from_claims(claims: dict[str, object]) -> str | None:
    raw = str(claims.get("phone") or "").strip()
    if not raw:
        return None
    return raw if raw.startswith("+") else f"+{raw}"


@router.get("/me")
def get_me(user: Annotated[CurrentUser, Depends(get_current_user)], db: UserDb) -> MeResponse:
    row = db.scalar(select(User).where(User.id == uuid.UUID(user.id)))
    return _to_response(row) if row else MeResponse(id=user.id)


@router.put("/me")
def put_me(
    body: ProfileUpdate,
    user: Annotated[CurrentUser, Depends(get_current_user)],
    db: UserDb,
    settings: Annotated[Settings, Depends(get_settings)],
    engine: Annotated[Engine, Depends(get_engine)],
) -> MeResponse:
    if settings.local_auth_stub:
        # Development only: mirror the Supabase account into the local auth.users stand-in.
        with engine.begin() as conn:
            conn.execute(
                text("INSERT INTO auth.users (id) VALUES (:id) ON CONFLICT DO NOTHING"),
                {"id": user.id},
            )
    phone = _phone_from_claims(user.claims)
    existing = db.scalar(select(User).where(User.id == uuid.UUID(user.id)))
    if existing is None and phone is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    values = {"name": body.name, "language_mode": body.language_mode, "roles": body.roles}
    stmt = insert(User).values(id=uuid.UUID(user.id), phone=phone or "", **values)
    try:
        with db.begin_nested():
            db.execute(stmt.on_conflict_do_update(index_elements=[User.id], set_=values))
    except IntegrityError as exc:
        # Another account already uses this phone number (phones are unique).
        raise api_error(ErrorCode.PHONE_IN_USE) from exc
    row = db.scalar(
        select(User).where(User.id == uuid.UUID(user.id)).execution_options(populate_existing=True)
    )
    if row is None:  # pragma: no cover - the upsert above guarantees a row
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    return _to_response(row)
