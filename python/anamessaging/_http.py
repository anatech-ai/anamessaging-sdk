"""Transport: token lifecycle, JSON / multipart encoding, error mapping, retries. Stdlib only."""
import json
import mimetypes
import socket
import threading
import time
import uuid
from datetime import datetime
from typing import Any, Dict, Optional, Tuple
from urllib import error as urlerror
from urllib import parse, request as urlrequest

from ._version import __version__
from .errors import AnaMessagingError, ConnectionError, RateLimitError, ServerError, error_from_response
from .models import FileAttachment

API_PREFIX = "/api/v1/anamsg"
REFRESH_MARGIN_SECONDS = 60


def _parse_iso(value: Optional[str]) -> Optional[float]:
    if not value:
        return None
    try:
        return datetime.fromisoformat(value.replace("Z", "+00:00")).timestamp()
    except ValueError:
        return None


def encode_multipart(fields: Dict[str, Any], attachment: FileAttachment, file_field: str = "file") -> Tuple[bytes, str]:
    """``multipart/form-data`` body: scalars as text, lists/dicts as JSON, the file in ``file``."""
    boundary = "----anamessaging" + uuid.uuid4().hex
    out = bytearray()
    for name, value in fields.items():
        if value is None:
            continue
        if isinstance(value, (list, dict)):
            value = json.dumps(value)
        elif isinstance(value, bool):
            value = "true" if value else "false"
        out += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n" % (boundary, name, value)).encode()
    ctype = attachment.content_type or mimetypes.guess_type(attachment.filename)[0] or "application/octet-stream"
    filename = attachment.filename.replace('"', "")
    out += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\nContent-Type: %s\r\n\r\n"
            % (boundary, file_field, filename, ctype)).encode()
    out += attachment.data + b"\r\n" + ("--%s--\r\n" % boundary).encode()
    return bytes(out), "multipart/form-data; boundary=" + boundary


class HttpClient:
    def __init__(self, base_url: str, client_id: Optional[str], client_secret: Optional[str],
                 scope: Optional[str], access_token: Optional[str], csrf_token: Optional[str],
                 timeout: float, max_retries: int) -> None:
        self.base_url = base_url.rstrip("/")
        self.client_id = client_id
        self.client_secret = client_secret
        self.scope = scope
        self.timeout = timeout
        self.max_retries = max_retries
        self._lock = threading.Lock()
        self._token: Optional[Dict[str, Any]] = None
        if access_token and not (client_id and client_secret):
            self._token = {"access": access_token, "csrf": csrf_token or "", "expires_at": float("inf")}

    @property
    def can_refresh(self) -> bool:
        return bool(self.client_id and self.client_secret)

    # ── tokens ──
    def fetch_token(self) -> Dict[str, Any]:
        if not self.can_refresh:
            raise AnaMessagingError("client_id and client_secret are required to obtain a token")
        body = {"grant_type": "client_credentials", "client_id": self.client_id, "client_secret": self.client_secret}
        if self.scope:
            body["scope"] = self.scope
        return self._send("POST", "/oauth2", json_body=body, auth=False)

    def get_token(self) -> Dict[str, Any]:
        with self._lock:  # thread-safe: Odoo / Frappe workers share one client
            if self._token and self._token["expires_at"] - REFRESH_MARGIN_SECONDS > time.time():
                return self._token
            t = self.fetch_token()
            expires_at = _parse_iso(t.get("expires_at")) or time.time() + float(t.get("expires_in", 0))
            self._token = {"access": t["access_token"], "csrf": t.get("csrf_token", ""), "expires_at": expires_at}
            return self._token

    def invalidate_token(self) -> None:
        with self._lock:
            if self._token and self._token["expires_at"] != float("inf"):
                self._token = None

    # ── requests ──
    def request(self, method: str, path: str, json_body: Any = None, query: Optional[Dict[str, Any]] = None,
                multipart: Optional[Tuple[bytes, str]] = None, auth: bool = True) -> Any:
        try:
            return self._send(method, path, json_body, query, multipart, auth)
        except AnaMessagingError as exc:
            if exc.status == 401 and auth and self.can_refresh:
                self.invalidate_token()
                return self._send(method, path, json_body, query, multipart, auth)
            raise

    def _send(self, method: str, path: str, json_body: Any = None, query: Optional[Dict[str, Any]] = None,
              multipart: Optional[Tuple[bytes, str]] = None, auth: bool = True) -> Any:
        url = self.base_url + API_PREFIX + path
        if query:
            q = {k: v for k, v in query.items() if v is not None}
            if q:
                url += "?" + parse.urlencode(q)

        attempt = 0
        while True:
            headers = {"Accept": "application/json", "User-Agent": "anamessaging-sdk-python/" + __version__}
            if auth:
                token = self.get_token()
                headers["Authorization"] = "Bearer " + token["access"]
                if token["csrf"]:
                    headers["x-csrf-token"] = token["csrf"]
            data = None
            if multipart is not None:
                data, headers["Content-Type"] = multipart
            elif json_body is not None:
                data = json.dumps(json_body).encode("utf-8")
                headers["Content-Type"] = "application/json"

            req = urlrequest.Request(url, data=data, headers=headers, method=method)
            try:
                with urlrequest.urlopen(req, timeout=self.timeout) as resp:
                    return self._decode(resp.read())
            except urlerror.HTTPError as http_err:
                try:
                    body = self._decode(http_err.read())
                finally:
                    http_err.close()
                exc = error_from_response(http_err.code, body, http_err.headers.get("Retry-After"))
            except (urlerror.URLError, socket.timeout, OSError) as net_err:
                raise ConnectionError("Could not reach ANA Messaging at %s: %s" % (self.base_url, net_err))

            retryable = (isinstance(exc, RateLimitError) and exc.code != "QUOTA_EXHAUSTED") or (
                isinstance(exc, ServerError) and exc.status == 503)
            if not retryable or attempt >= self.max_retries:
                raise exc
            wait = getattr(exc, "retry_after_seconds", None)
            time.sleep(wait if wait is not None else 0.5 * (2 ** attempt))
            attempt += 1

    @staticmethod
    def _decode(raw: bytes) -> Any:
        if not raw:
            return None
        text = raw.decode("utf-8", errors="replace")
        try:
            return json.loads(text)
        except ValueError:
            return text
