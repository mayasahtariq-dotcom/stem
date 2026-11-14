import uuid
from typing import Annotated, Literal

from fastapi import APIRouter, Depends
from pydantic import BaseModel, Field, field_validator
from sqlalchemy import select
from sqlalchemy.dialects.postgresql import insert

from app.auth import CurrentUser, get_current_user
from app.db.models import User
from app.db.session import UserDb
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
) -> MeResponse:
    phone = _phone_from_claims(user.claims)
    existing = db.scalar(select(User).where(User.id == uuid.UUID(user.id)))
    if existing is None and phone is None:
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    values = {"name": body.name, "language_mode": body.language_mode, "roles": body.roles}
    stmt = insert(User).values(id=uuid.UUID(user.id), phone=phone or "", **values)
    db.execute(stmt.on_conflict_do_update(index_elements=[User.id], set_=values))
    row = db.scalar(
        select(User).where(User.id == uuid.UUID(user.id)).execution_options(populate_existing=True)
    )
    if row is None:  # pragma: no cover - the upsert above guarantees a row
        raise api_error(ErrorCode.PROFILE_INCOMPLETE)
    return _to_response(row)
