"""ANA Messaging SDK - send WhatsApp and SMS through the ANA Messaging API.

    from anamessaging import AnaMessagingClient, TemplateComponent

    ana = AnaMessagingClient(client_id="...", client_secret="...")
    ana.whatsapp.send_template("919876543210", "order_shipped",
                               components=[TemplateComponent.body("Ravi", "SO-1042")])
"""
from ._version import __version__
from .client import (
    PRODUCTION_BASE_URL, SANDBOX_BASE_URL, AnaMessagingClient, Campaigns, Media, Sms, WhatsApp,
)
from .errors import (
    AnaMessagingError, AuthenticationError, ConnectionError, NotFoundError, PermissionDeniedError,
    QuotaExceededError, RateLimitError, ServerError, SubscriptionInactiveError, ValidationError,
)
from .models import (
    MEDIA_TYPES, ApiResponse, BulkDispatchSummary, FileAttachment, MediaUploadResult, MessageDispatchResult,
    OAuth2Token, RunCampaignRequest, RunCampaignResult, SendMediaRequest, SendSmsRequest, SendSmsResult,
    SendTemplateRequest, SendTextRequest, SendWhatsAppBulkRequest, TemplateComponent, TemplateParameter,
)

__all__ = [
    "__version__", "AnaMessagingClient", "PRODUCTION_BASE_URL", "SANDBOX_BASE_URL",
    "WhatsApp", "Sms", "Campaigns", "Media",
    "AnaMessagingError", "AuthenticationError", "ConnectionError", "NotFoundError", "PermissionDeniedError",
    "QuotaExceededError", "RateLimitError", "ServerError", "SubscriptionInactiveError", "ValidationError",
    "MEDIA_TYPES", "ApiResponse", "BulkDispatchSummary", "FileAttachment", "MediaUploadResult",
    "MessageDispatchResult", "OAuth2Token", "RunCampaignRequest", "RunCampaignResult", "SendMediaRequest",
    "SendSmsRequest", "SendSmsResult", "SendTemplateRequest", "SendTextRequest", "SendWhatsAppBulkRequest",
    "TemplateComponent", "TemplateParameter",
]
