from collections.abc import Iterator

from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.config import get_settings


class Base(DeclarativeBase):
    pass


def make_sessionmaker(database_url: str | None = None) -> sessionmaker[Session]:
    engine = create_engine(database_url or get_settings().database_url, pool_pre_ping=True)
    return sessionmaker(engine, expire_on_commit=False)


def get_session() -> Iterator[Session]:
    with make_sessionmaker()() as session:
        yield session
