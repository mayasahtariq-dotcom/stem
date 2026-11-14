"""Database sessions.

`user_session` runs every statement as the Postgres `authenticated` role with the caller's
JWT claims set, so Row-Level Security applies to backend queries too: a second layer behind
the endpoint checks. `service_session` uses the connection's own (elevated) role and is only
for operations the PRD marks as elevated, such as the class preview.
"""

import json
from collections.abc import Iterator
from functools import lru_cache
from typing import Annotated

from fastapi import Depends
from sqlalchemy import Engine, create_engine, text
from sqlalchemy.orm import Session

from app.auth import CurrentUser, get_current_user
from app.config import Settings, get_settings


@lru_cache(maxsize=4)
def _engine_for(url: str) -> Engine:
    return create_engine(url, pool_pre_ping=True)


def get_engine(settings: Annotated[Settings, Depends(get_settings)]) -> Engine:
    return _engine_for(settings.database_url)


def act_as(session: Session, user_id: str) -> None:
    """Switch the current transaction to the `authenticated` role for [user_id]."""
    claims = json.dumps({"sub": user_id, "role": "authenticated"})
    session.execute(text("SET LOCAL ROLE authenticated"))
    session.execute(
        text(
            "SELECT set_config('request.jwt.claims', :claims, true),"
            " set_config('request.jwt.claim.sub', :sub, true)"
        ),
        {"claims": claims, "sub": user_id},
    )


def user_session(
    user: Annotated[CurrentUser, Depends(get_current_user)],
    engine: Annotated[Engine, Depends(get_engine)],
) -> Iterator[Session]:
    with Session(engine) as session, session.begin():
        act_as(session, user.id)
        yield session


def service_session(engine: Annotated[Engine, Depends(get_engine)]) -> Iterator[Session]:
    with Session(engine) as session, session.begin():
        yield session


UserDb = Annotated[Session, Depends(user_session)]
ServiceDb = Annotated[Session, Depends(service_session)]
