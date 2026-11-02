"""Exceptions raised by the ANA Messaging SDK."""
from typing import Any, List, Optional


class AnaMessagingError(Exception):
    """Base class of every SDK error.

    Attributes:
        status: HTTP status, or 0 when the API could not be reached.
        code: machine-readable reason, e.g. ``QUOTA_EXHAUSTED`` or ``invalid_client``.
        errors: field-level validation messages.
        body: raw response body.
    """

    def __init__(self, message: str, status: int = 0, code: Optional[str] = None,
                 errors: Optional[List[str]] = None, body: Any = None) -> None:
        super().__init__(message)
        self.message = message
        self.status = status
        self.code = code
        self.errors = errors
        self.body = body

    def __str__(self) -> str:
        parts = [self.message]
        if self.status:
            parts.insert(0, "[%s]" % self.status)
        if self.code:
            parts.append("(%s)" % self.code)
        if self.errors:
            parts.append("- " + "; ".join(self.errors))
        return " ".join(parts)


class ValidationError(AnaMessagingError):
    """400 / 413: the request was rejected; fix the payload."""


class AuthenticationError(AnaMessagingError):
    """401: wrong client id / secret, or the token is invalid, expired or revoked."""


class PermissionDeniedError(AnaMessagingError):
    """403: CSRF, IP allowlist or domain check failed."""


class SubscriptionInactiveError(PermissionDeniedError):
    """403: no active ANA Messaging subscription. Renew on anamessaging.com."""


class NotFoundError(AnaMessagingError):
    """404: e.g. unknown template or campaign."""


class RateLimitError(AnaMessagingError):
    """429: too many requests. ``retry_after_seconds`` says when to retry."""

    retry_after_seconds: Optional[float] = None


class QuotaExceededError(RateLimitError):
    """429 ``QUOTA_EXHAUSTED``: the subscription's allowance is used up."""


class ServerError(AnaMessagingError):
    """5xx."""


class ConnectionError(AnaMessagingError):  # noqa: A001 - deliberate, scoped to this package
    """Network failure or timeout."""


_SUBSCRIPTION_CODES = {"NO_SUBSCRIPTION", "SUBSCRIPTION_EXPIRED"}


def error_from_response(status: int, body: Any, retry_after: Optional[str] = None) -> AnaMessagingError:
    """Build the matching exception subclass for an HTTP error response."""
    data = body if isinstance(body, dict) else {}
    code = data.get("code") or data.get("error")
    message = data.get("error_description") or data.get("message") or (
        body if isinstance(body, str) and body else "HTTP %s" % status)
    errors = data.get("errors") if isinstance(data.get("errors"), list) else None
    args = (message, status, code, errors, body)

    if status in (400, 413, 422):
        return ValidationError(*args)
    if status == 401:
        return AuthenticationError(*args)
    if status == 403:
        lowered = str(message).lower()
        if code in _SUBSCRIPTION_CODES or "subscription" in lowered or "api access is not enabled" in lowered:
            return SubscriptionInactiveError(*args)
        return PermissionDeniedError(*args)
    if status == 404:
        return NotFoundError(*args)
    if status == 429:
        err = QuotaExceededError(*args) if code == "QUOTA_EXHAUSTED" else RateLimitError(*args)
        try:
            err.retry_after_seconds = float(retry_after) if retry_after is not None else None
        except ValueError:
            pass
        return err
    if status >= 500:
        return ServerError(*args)
    return AnaMessagingError(*args)
