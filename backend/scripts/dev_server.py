"""Run the backend on this PC for testing with a phone on the same Wi-Fi.

    cd backend
    .venv/Scripts/python scripts/dev_server.py            # real AI (needs the [ml] extra)
    .venv/Scripts/python scripts/dev_server.py --fake-ai  # sample course, no models
    add --real-embeddings to also load BGE-M3 (2.2 GB download; doubt chat needs it in M3)

It:
  1. starts an embedded Postgres in backend/var/pgdata (data is kept between runs),
  2. applies the database migrations,
  3. serves the API on http://<this PC's Wi-Fi address>:8000 with uploads stored locally,
  4. prints the address to type into the app's Profile → Server setting.

Logins are checked against your real Supabase project (SUPABASE_URL in .env). Everything else
lives on this PC. Not for production.
"""

import os
import socket
import sys
from pathlib import Path

BACKEND = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(BACKEND))
os.chdir(BACKEND)


def lan_ip() -> str:
    """The address other devices on the Wi-Fi use to reach this PC."""
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as s:
        try:
            s.connect(("10.255.255.255", 1))  # no packets are sent; just picks the route
            return str(s.getsockname()[0])
        except OSError:
            return "127.0.0.1"


def main() -> None:
    fake_ai = "--fake-ai" in sys.argv
    real_embeddings = "--real-embeddings" in sys.argv  # downloads BGE-M3 (2.2 GB); M3 needs it
    port = 8000

    # Use Windows' certificate store for every HTTPS call (model downloads, OpenRouter,
    # Supabase), so antivirus or network filters that inspect HTTPS don't break them.
    try:
        import truststore

        truststore.inject_into_ssl()
    except ImportError:
        pass

    import pgserver

    datadir = BACKEND / "var" / "pgdata"
    datadir.mkdir(parents=True, exist_ok=True)
    print("Starting the local database…")
    server = getattr(pgserver, "get_server")(str(datadir), cleanup_mode="stop")  # noqa: B009
    db_url = str(server.get_uri()).replace("postgresql://", "postgresql+psycopg://", 1)

    ip = lan_ip()
    os.environ.update(
        {
            "DATABASE_URL": db_url,
            "USE_FAKE_AI": "true" if fake_ai else "false",
            "STORAGE_BACKEND": "local",
            "LOCAL_STORAGE_DIR": str(BACKEND / "var" / "uploads"),
            "PUBLIC_BASE_URL": f"http://{ip}:{port}",
            "JOBS_INLINE": "true",
            "LOCAL_AUTH_STUB": "true",
            "FAKE_EMBEDDINGS": "false" if real_embeddings else "true",
        }
    )

    from alembic.config import Config

    from alembic import command

    print("Applying database migrations…")
    cfg = Config(str(BACKEND / "alembic.ini"))
    cfg.set_main_option("script_location", str(BACKEND / "alembic"))
    command.upgrade(cfg, "head")

    if not fake_ai:
        print(
            "Loading the AI models (the first run downloads them; this can take several minutes)…"
        )
        from app.ai.providers import get_services

        get_services()

    print()
    print("=" * 64)
    print("  Backend running. In the Mooshala app: Profile → Server →")
    print(f"      Real server:  http://{ip}:{port}/")
    print("  (Phone and PC must be on the same Wi-Fi. If Windows asks about")
    print("   the firewall for Python, allow it on private networks.)")
    print(f"  AI: {'sample course (fake)' if fake_ai else 'real (Surya + OpenRouter)'}")
    print("  Press Ctrl+C to stop.")
    print("=" * 64)

    import uvicorn

    uvicorn.run("app.main:app", host="0.0.0.0", port=port, log_level="info")  # noqa: S104


if __name__ == "__main__":
    main()
