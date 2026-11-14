from typing import Annotated

from fastapi import APIRouter, Depends
from pydantic import BaseModel

from app.auth import CurrentUser, get_current_user

router = APIRouter(tags=["me"])


class MeResponse(BaseModel):
    id: str


@router.get("/me")
def me(user: Annotated[CurrentUser, Depends(get_current_user)]) -> MeResponse:
    return MeResponse(id=user.id)
