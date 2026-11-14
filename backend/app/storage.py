"""Private storage for note photos and PDFs, at `{owner}/{upload}/{page}.{ext}`.

The app uploads each page straight to storage with a short-lived signed link, then the
pipeline reads it back. Supabase Storage in production; LocalStorage (files on disk, upload
through this API) for development and tests.
"""

import hashlib
import hmac
import time
from dataclasses import dataclass, field
from pathlib import Path
from typing import Protocol

import httpx


@dataclass(frozen=True)
class UploadTarget:
    url: str
    method: str = "PUT"
    headers: dict[str, str] = field(default_factory=dict)


class StorageError(Exception):
    pass


class StorageProvider(Protocol):
    def upload_target(self, path: str, content_type: str) -> UploadTarget: ...

    def read(self, path: str) -> bytes: ...

    def exists(self, path: str) -> bool: ...


class LocalStorage:
    """Files under [root]. Upload links point at `PUT /local-storage/{token}` on this API; the
    token is an HMAC-signed path with an expiry, so only links we issued can write."""

    TTL_SECONDS = 15 * 60

    def __init__(self, root: str, public_base_url: str, secret: str) -> None:
        self.root = Path(root)
        self._base = public_base_url.rstrip("/")
        self._secret = (secret or "dev-local-storage").encode()

    def _sign(self, payload: str) -> str:
        return hmac.new(self._secret, payload.encode(), hashlib.sha256).hexdigest()[:32]

    def make_token(self, path: str, now: float | None = None) -> str:
        expires = int((now or time.time()) + self.TTL_SECONDS)
        payload = f"{path}|{expires}"
        return f"{payload.replace('/', '~')}|{self._sign(payload)}"

    def path_from_token(self, token: str, now: float | None = None) -> str | None:
        try:
            path_enc, expires, sig = token.rsplit("|", 2)
        except ValueError:
            return None
        path = path_enc.replace("~", "/")
        if not hmac.compare_digest(sig, self._sign(f"{path}|{expires}")):
            return None
        if int(expires) < (now or time.time()) or ".." in path:
            return None
        return path

    def upload_target(self, path: str, content_type: str) -> UploadTarget:
        return UploadTarget(
            url=f"{self._base}/local-storage/{self.make_token(path)}",
            headers={"Content-Type": content_type},
        )

    def write(self, path: str, data: bytes) -> None:
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)

    def read(self, path: str) -> bytes:
        try:
            return (self.root / path).read_bytes()
        except OSError as exc:
            raise StorageError(f"missing {path}") from exc

    def exists(self, path: str) -> bool:
        return (self.root / path).is_file()


class SupabaseStorage:
    """Supabase Storage REST API with the service key (server side only).

    VERIFY against current Supabase docs: signed upload URLs are created with
    `POST /storage/v1/object/upload/sign/{bucket}/{path}` and the client then PUTs the file to
    the returned URL.
    """

    def __init__(self, supabase_url: str, service_key: str, bucket: str) -> None:
        self._base = supabase_url.rstrip("/") + "/storage/v1"
        self._bucket = bucket
        self._headers = {"Authorization": f"Bearer {service_key}", "apikey": service_key}

    def upload_target(self, path: str, content_type: str) -> UploadTarget:
        r = httpx.post(
            f"{self._base}/object/upload/sign/{self._bucket}/{path}",
            headers=self._headers,
            timeout=20,
        )
        if r.status_code >= 400:
            raise StorageError(f"could not sign upload: {r.status_code}")
        signed = r.json().get("url", "")
        return UploadTarget(url=f"{self._base}{signed}", headers={"Content-Type": content_type})

    def read(self, path: str) -> bytes:
        r = httpx.get(
            f"{self._base}/object/{self._bucket}/{path}", headers=self._headers, timeout=60
        )
        if r.status_code >= 400:
            raise StorageError(f"missing {path}: {r.status_code}")
        return r.content

    def exists(self, path: str) -> bool:
        r = httpx.head(
            f"{self._base}/object/{self._bucket}/{path}", headers=self._headers, timeout=20
        )
        return r.status_code < 400
