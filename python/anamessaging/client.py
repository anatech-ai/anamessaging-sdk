"""ANA Messaging API client."""
import os
from typing import Any, Dict, List, Optional, Union

from ._http import HttpClient, encode_multipart
from .models import (
    ApiResponse, BulkDispatchSummary, Components, FileAttachment, MediaUploadResult, MessageDispatchResult,
    OAuth2Token, RunCampaignRequest, RunCampaignResult, SendMediaRequest, SendSmsRequest, SendSmsResult,
    SendTemplateRequest, SendTextRequest, SendWhatsAppBulkRequest, _clean,
)

PRODUCTION_BASE_URL = "https://api.anamessaging.com"
SANDBOX_BASE_URL = "https://api-dev.anamessaging.com"

RequestLike = Union[Dict[str, Any], Any]


def _body(req: RequestLike) -> Dict[str, Any]:
    return req.to_dict() if hasattr(req, "to_dict") else _clean(dict(req))


class WhatsApp:
    """WhatsApp sends. Calls return once the message is *queued*, not delivered."""

    def __init__(self, http: HttpClient) -> None:
        self._http = http

    def send_text(self, to: str, text: str, campaign_id: Optional[str] = None) -> ApiResponse[MessageDispatchResult]:
        return self.send(SendTextRequest(to=to, text=text, campaignId=campaign_id))

    def send_template(self, to: str, template_name: str, language: str = "en",
                      components: Optional[Components] = None, attachment: Optional[FileAttachment] = None,
                      campaign_id: Optional[str] = None) -> ApiResponse[MessageDispatchResult]:
        """Send an approved template. Pass ``attachment`` for an image/video/document header."""
        req = SendTemplateRequest(to=to, templateName=template_name, language=language,
                                  components=components, campaignId=campaign_id)
        return self.send(req, attachment)

    def send_media(self, to: str, media_type: Optional[str] = None, media_url: Optional[str] = None,
                   media_upload_id: Optional[int] = None, caption: Optional[str] = None,
                   filename: Optional[str] = None, campaign_id: Optional[str] = None
                   ) -> ApiResponse[MessageDispatchResult]:
        if not media_url and media_upload_id is None:
            raise ValueError("send_media needs media_url or media_upload_id")
        return self.send(SendMediaRequest(to=to, mediaType=media_type, mediaUrl=media_url,
                                          mediaUploadId=media_upload_id, caption=caption, filename=filename,
                                          campaignId=campaign_id))

    def send(self, request: RequestLike, attachment: Optional[FileAttachment] = None
             ) -> ApiResponse[MessageDispatchResult]:
        """Raw ``POST /send/whatsapp`` with a request model or a dict."""
        body = _body(request)
        if attachment is not None:
            res = self._http.request("POST", "/send/whatsapp", multipart=encode_multipart(body, attachment))
        else:
            res = self._http.request("POST", "/send/whatsapp", json_body=body)
        return ApiResponse.parse(res, MessageDispatchResult.from_dict)

    def send_bulk(self, request: RequestLike, attachment: Optional[FileAttachment] = None
                  ) -> ApiResponse[BulkDispatchSummary]:
        """One template to many recipients (not recorded as a campaign)."""
        body = _body(request)
        if attachment is not None:
            res = self._http.request("POST", "/send/whatsapp/bulk", multipart=encode_multipart(body, attachment))
        else:
            res = self._http.request("POST", "/send/whatsapp/bulk", json_body=body)
        return ApiResponse.parse(res, BulkDispatchSummary.from_dict)


class Sms:
    def __init__(self, http: HttpClient) -> None:
        self._http = http

    def send(self, request: Optional[RequestLike] = None, *, to: Optional[str] = None,
             message: Optional[str] = None, recipients: Optional[List[str]] = None, **extra: Any
             ) -> ApiResponse[SendSmsResult]:
        """``sms.send(to="91...", message="...")`` or ``sms.send(SendSmsRequest(...))``.

        Extra keyword args: ``senderId``, ``principalEntityId``, ``dltTemplateId``, ``unicode``, ``campaignId``.
        """
        if request is None:
            if message is None:
                raise ValueError("SMS needs a message")
            request = SendSmsRequest(message=message, to=to, recipients=recipients, **extra)
        body = _body(request)
        if not body.get("to") and not body.get("recipients"):
            raise ValueError("SMS needs `to` or `recipients`")
        res = self._http.request("POST", "/sms/sent", json_body=body)
        return ApiResponse.parse(res, SendSmsResult.from_dict)


class Campaigns:
    def __init__(self, http: HttpClient) -> None:
        self._http = http

    def run(self, request: RequestLike, attachment: Optional[FileAttachment] = None
            ) -> ApiResponse[RunCampaignResult]:
        body = _body(request)
        if attachment is not None:
            res = self._http.request("POST", "/campaigns", multipart=encode_multipart(body, attachment))
        else:
            res = self._http.request("POST", "/campaigns", json_body=body)
        return ApiResponse.parse(res, RunCampaignResult.from_dict)

    def list(self, limit: Optional[int] = None, offset: Optional[int] = None) -> ApiResponse[Dict[str, Any]]:
        return ApiResponse.parse(self._http.request("GET", "/campaigns", query={"limit": limit, "offset": offset}))

    def get(self, campaign_id: Union[int, str]) -> ApiResponse[Dict[str, Any]]:
        from urllib.parse import quote

        return ApiResponse.parse(self._http.request("GET", "/campaigns/" + quote(str(campaign_id), safe="")))


class Media:
    def __init__(self, http: HttpClient) -> None:
        self._http = http

    def upload(self, file: FileAttachment, media_type: Optional[str] = None) -> ApiResponse[MediaUploadResult]:
        """Upload once, send many times with ``media_upload_id``."""
        fields = {"mediaType": media_type} if media_type else {}
        res = self._http.request("POST", "/media", multipart=encode_multipart(fields, file))
        return ApiResponse.parse(res, MediaUploadResult.from_dict)


class AnaMessagingClient:
    """ANA Messaging client.

    >>> ana = AnaMessagingClient(client_id="...", client_secret="...")
    >>> ana.whatsapp.send_text("919876543210", "Hello")
    >>> ana.sms.send(to="919876543210", message="Your OTP is 1234")

    Credentials are issued on anamessaging.com to organisations with an **active API
    subscription**. Without one every call raises ``SubscriptionInactiveError``.
    Environment fallbacks: ``ANAMESSAGING_CLIENT_ID``, ``ANAMESSAGING_CLIENT_SECRET``,
    ``ANAMESSAGING_SCOPE``, ``ANAMESSAGING_BASE_URL``, ``ANAMESSAGING_ACCESS_TOKEN``, ``ANAMESSAGING_CSRF_TOKEN``.
    The client is thread-safe; create one and reuse it.
    """

    def __init__(self, client_id: Optional[str] = None, client_secret: Optional[str] = None, *,
                 scope: Optional[str] = None, base_url: Optional[str] = None,
                 access_token: Optional[str] = None, csrf_token: Optional[str] = None,
                 timeout: float = 30.0, max_retries: int = 2) -> None:
        env = os.environ
        client_id = client_id or env.get("ANAMESSAGING_CLIENT_ID")
        client_secret = client_secret or env.get("ANAMESSAGING_CLIENT_SECRET")
        access_token = access_token or env.get("ANAMESSAGING_ACCESS_TOKEN")
        if not (client_id and client_secret) and not access_token:
            raise ValueError("ANA Messaging SDK: set client_id and client_secret (issued on anamessaging.com "
                             "with an active subscription), or access_token and csrf_token.")
        self._http = HttpClient(
            base_url=base_url or env.get("ANAMESSAGING_BASE_URL") or PRODUCTION_BASE_URL,
            client_id=client_id, client_secret=client_secret, scope=scope or env.get("ANAMESSAGING_SCOPE"),
            access_token=access_token, csrf_token=csrf_token or env.get("ANAMESSAGING_CSRF_TOKEN"),
            timeout=timeout, max_retries=max_retries,
        )
        self.whatsapp = WhatsApp(self._http)
        self.sms = Sms(self._http)
        self.campaigns = Campaigns(self._http)
        self.media = Media(self._http)

    def authenticate(self) -> OAuth2Token:
        """Fetch a token now - validates credentials and subscription at startup."""
        return OAuth2Token.from_dict(self._http.fetch_token())

    def health(self) -> Dict[str, Any]:
        """API health; needs no credentials."""
        return self._http.request("GET", "/health", auth=False)
