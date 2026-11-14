"""Production job queue on procrastinate (Postgres-backed), used when JOBS_INLINE=false.

Run a worker with:  procrastinate --app=app.jobs.queue.queue worker
Create its tables once with:  procrastinate --app=app.jobs.queue.queue schema --apply

VERIFY against the installed procrastinate version's docs before relying on this in
production; tests and development run jobs inline instead.
"""

from typing import Any

from app.config import get_settings

try:  # pragma: no cover - exercised only with a real queue
    import procrastinate

    _conninfo = get_settings().database_url.replace("postgresql+psycopg://", "postgresql://", 1)
    queue = procrastinate.App(connector=procrastinate.PsycopgConnector(conninfo=_conninfo))

    @queue.task(name="run_job", queue="pipeline")
    def run_job(name: str, kwargs: dict[str, Any]) -> None:
        from app.ai.providers import get_services

        services = get_services()
        services.jobs.inline = True  # inside the worker, run the handler directly
        services.jobs.enqueue(name, **kwargs)

except ImportError:  # pragma: no cover
    queue = None  # type: ignore[assignment]


def defer(name: str, **kwargs: Any) -> None:  # pragma: no cover - production path
    if queue is None:
        raise RuntimeError("procrastinate is not installed")
    with queue.open():
        run_job.defer(name=name, kwargs=kwargs)
