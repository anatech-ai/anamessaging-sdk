"""Request and response models (DTOs) of the ANA Messaging API.

Request models serialise with :meth:`to_dict` (``None`` fields are dropped). Response models are
built with ``from_dict`` and ignore fields they do not know, so newer API versions do not break
older SDKs. Plain ``dict`` payloads are accepted everywhere a request model is.
"""
from dataclasses import asdict, dataclass, field, fields
from typing import Any, Dict, Generic, List, Optional, TypeVar, Union

T = TypeVar("T")

MEDIA_TYPES = ("image", "video", "document", "audio")


def _clean(value: Any) -> Any:
    if isinstance(value, dict):
        return {k: _clean(v) for k, v in value.items() if v is not None}
    if isinstance(value, list):
        return [_clean(v) for v in value]
    return value


class _Request:
    def to_dict(self) -> Dict[str, Any]:
        return _clean(asdict(self))  # type: ignore[call-overload]


def _pick(cls: Any, data: Optional[Dict[str, Any]]) -> Dict[str, Any]:
    names = {f.name for f in fields(cls)}
    return {k: v for k, v in (data or {}).items() if k in names}


# ─── Shared ───────────────────────────────────────────────────────────────


@dataclass
class TemplateParameter:
    """One template parameter, WhatsApp Cloud API format. ``type`` is usually ``text``."""

    type: str = "text"
    text: Optional[str] = None
    payload: Optional[str] = None
    image: Optional[Dict[str, Any]] = None
    video: Optional[Dict[str, Any]] = None
    document: Optional[Dict[str, Any]] = None
    currency: Optional[Dict[str, Any]] = None
    date_time: Optional[Dict[str, Any]] = None


@dataclass
class TemplateComponent:
    """Header, body or button parameters of a template.

    Example: ``TemplateComponent.body("Ravi", "INV-001")`` fills ``{{1}}`` and ``{{2}}`` of the body.
    """

    type: str
    parameters: List[TemplateParameter] = field(default_factory=list)
    sub_type: Optional[str] = None
    index: Optional[str] = None

    @classmethod
    def body(cls, *texts: str) -> "TemplateComponent":
        return cls("body", [TemplateParameter("text", text=str(t)) for t in texts])

    @classmethod
    def header_text(cls, text: str) -> "TemplateComponent":
        return cls("header", [TemplateParameter("text", text=str(text))])

    @classmethod
    def url_button(cls, index: int, suffix: str) -> "TemplateComponent":
        return cls("button", [TemplateParameter("text", text=str(suffix))], sub_type="url", index=str(index))


Components = List[Union[TemplateComponent, Dict[str, Any]]]


@dataclass
class FileAttachment:
    """A file sent as ``multipart/form-data`` (template media header, or media upload)."""

    data: bytes
    filename: str
    content_type: Optional[str] = None

    @classmethod
    def from_path(cls, path: str, content_type: Optional[str] = None) -> "FileAttachment":
        import os

        with open(path, "rb") as fh:
            return cls(fh.read(), os.path.basename(path), content_type)


# ─── Requests ─────────────────────────────────────────────────────────────


@dataclass
class SendTextRequest(_Request):
    """WhatsApp text. Only inside the 24h customer-service window."""

    to: str
    text: str
    campaignId: Optional[str] = None


@dataclass
class SendTemplateRequest(_Request):
    """Approved WhatsApp template. ``language`` defaults to ``en``."""

    to: str
    templateName: str
    language: str = "en"
    components: Optional[Components] = None
    campaignId: Optional[str] = None


@dataclass
class SendMediaRequest(_Request):
    """Image, video, document or audio by ``mediaUrl`` (public) or ``mediaUploadId``."""

    to: str
    mediaType: Optional[str] = None
    mediaUrl: Optional[str] = None
    mediaUploadId: Optional[int] = None
    caption: Optional[str] = None
    filename: Optional[str] = None
    campaignId: Optional[str] = None


@dataclass
class SendWhatsAppBulkRequest(_Request):
    """One template to many recipients: ``recipients``, else ``customerIds``, else all active customers."""

    templateName: str
    language: str = "en"
    components: Optional[Components] = None
    recipients: Optional[List[str]] = None
    customerIds: Optional[List[int]] = None
    useBulkPubSub: Optional[bool] = None
    campaignId: Optional[str] = None


@dataclass
class RunCampaignRequest(_Request):
    """Recorded campaign (opt-in and 24h rules applied). ``name`` shows on the portal."""

    templateName: str
    language: str = "en"
    components: Optional[Components] = None
    recipients: Optional[List[str]] = None
    customerIds: Optional[List[int]] = None
    useBulkPubSub: Optional[bool] = None
    name: Optional[str] = None


@dataclass
class SendSmsRequest(_Request):
    """SMS to ``to`` or ``recipients``. Indian A2P needs ``principalEntityId`` + ``dltTemplateId``."""

    message: str
    to: Optional[str] = None
    recipients: Optional[List[str]] = None
    senderId: Optional[str] = None
    principalEntityId: Optional[str] = None
    dltTemplateId: Optional[str] = None
    unicode: Optional[bool] = None
    campaignId: Optional[str] = None


# ─── Responses ────────────────────────────────────────────────────────────


@dataclass
class OAuth2Token:
    access_token: str
    token_type: str
    expires_in: int
    csrf_token: str
    expires_at: Optional[str] = None
    scope: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "OAuth2Token":
        return cls(**_pick(cls, data))


@dataclass
class MessageDispatchResult:
    """One recipient's outcome. ``status`` ``queued`` = accepted (not yet delivered)."""

    messageId: Union[int, str, None] = None
    to: Optional[str] = None
    status: Optional[str] = None
    pubSubMessageId: Optional[str] = None
    error: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "MessageDispatchResult":
        return cls(**_pick(cls, data))


@dataclass
class SendSmsResult:
    total: int = 0
    messages: List[MessageDispatchResult] = field(default_factory=list)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "SendSmsResult":
        return cls(data.get("total", 0), [MessageDispatchResult.from_dict(m) for m in data.get("messages") or []])


@dataclass
class BulkDispatchSummary:
    campaignSize: int = 0
    queued: int = 0
    failed: int = 0
    messages: List[MessageDispatchResult] = field(default_factory=list)
    errors: List[Dict[str, Any]] = field(default_factory=list)
    pubSubMessageId: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "BulkDispatchSummary":
        out = cls(**_pick(cls, data))
        out.messages = [MessageDispatchResult.from_dict(m) for m in data.get("messages") or []]
        return out


@dataclass
class RunCampaignResult:
    campaignId: Optional[str] = None
    status: Optional[str] = None
    campaignSize: int = 0
    queued: int = 0
    skipped: int = 0
    skippedRecipients: List[Dict[str, Any]] = field(default_factory=list)
    failed: int = 0
    errors: List[Dict[str, Any]] = field(default_factory=list)
    category: Optional[str] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "RunCampaignResult":
        return cls(**_pick(cls, data))


@dataclass
class MediaUploadResult:
    mediaUploadId: Optional[int] = None
    mediaType: Optional[str] = None
    mimeType: Optional[str] = None
    filename: Optional[str] = None
    size: Optional[int] = None

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "MediaUploadResult":
        return cls(**_pick(cls, data))


@dataclass
class ApiResponse(Generic[T]):
    """Envelope of every API response. ``data`` is the typed payload; ``raw`` the full JSON."""

    success: bool
    message: Optional[str] = None
    data: Optional[T] = None
    errors: Optional[List[str]] = None
    timestamp: Optional[str] = None
    raw: Dict[str, Any] = field(default_factory=dict, repr=False)

    @classmethod
    def parse(cls, body: Any, parse_data: Any = None) -> "ApiResponse[Any]":
        body = body if isinstance(body, dict) else {}
        data = body.get("data")
        if parse_data is not None and isinstance(data, dict):
            data = parse_data(data)
        return cls(bool(body.get("success")), body.get("message"), data, body.get("errors"),
                   body.get("timestamp"), body)
