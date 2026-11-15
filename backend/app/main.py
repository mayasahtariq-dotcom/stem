import logging

from fastapi import FastAPI

from app import __version__
from app.config import get_settings
from app.logging import configure_logging
from app.routers import classrooms, courses, dashboard, health, learning, me, parents


def create_app() -> FastAPI:
    settings = get_settings()
    configure_logging(settings.log_level)
    app = FastAPI(title="STEM Tutor API", version=__version__)
    app.include_router(health.router)
    app.include_router(me.router)
    app.include_router(classrooms.router)
    app.include_router(courses.router)
    app.include_router(learning.router)
    app.include_router(dashboard.router)
    app.include_router(parents.router)
    logging.getLogger(__name__).info("app started", extra={"version": __version__})
    return app


app = create_app()
