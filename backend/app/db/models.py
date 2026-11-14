import uuid
from datetime import datetime

from sqlalchemy import CheckConstraint, DateTime, String, Text, func, text
from sqlalchemy.dialects.postgresql import ARRAY, UUID
from sqlalchemy.orm import Mapped, mapped_column

from app.db.base import Base

ROLES = ("student", "teacher", "parent")
LANGUAGE_MODES = ("hindi", "kannada")


class User(Base):
    __tablename__ = "users"
    __table_args__ = (
        CheckConstraint(
            "roles <@ ARRAY['student','teacher','parent']::text[]", name="users_roles_valid"
        ),
        CheckConstraint("language_mode IN ('hindi','kannada')", name="users_language_mode_valid"),
    )

    # Same ID as the Supabase auth account (auth.users.id).
    id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), primary_key=True)
    name: Mapped[str] = mapped_column(Text, nullable=False, server_default="")
    phone: Mapped[str] = mapped_column(String(20), nullable=False, unique=True)
    roles: Mapped[list[str]] = mapped_column(
        ARRAY(Text), nullable=False, server_default=text("'{}'::text[]")
    )
    language_mode: Mapped[str] = mapped_column(Text, nullable=False, server_default="hindi")
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
