# anamessaging (Python)

Official ANA Messaging SDK for Python 3.8+. Send WhatsApp and SMS through the ANA Messaging API.
It uses **only the standard library**, so it installs into Odoo, ERPNext/Frappe, Django or any
script without dependency conflicts.

```bash
pip install anamessaging
```

You need a **client id and secret** from anamessaging.com (Settings → API Keys) and an **active API
subscription**. Without the subscription, calls raise `SubscriptionInactiveError`.

## Quick start

```python
from anamessaging import AnaMessagingClient, TemplateComponent, FileAttachment, SendSmsRequest

ana = AnaMessagingClient(client_id="...", client_secret="...")
# or AnaMessagingClient() with ANAMESSAGING_CLIENT_ID / ANAMESSAGING_CLIENT_SECRET set
# sandbox: AnaMessagingClient(..., base_url="https://api-dev.anamessaging.com")

# Text
ana.whatsapp.send_text("919876543210", "Your order has shipped.")

# Template: "Hi {{1}}, order {{2}} is confirmed."
res = ana.whatsapp.send_template(
    "919876543210", "order_confirmation",
    components=[TemplateComponent.body("Ravi", "SO-1042")],
    campaign_id="SO-1042",
)
print(res.success, res.data.messageId, res.data.status)   # True 90210 queued

# Template with a document header (invoice PDF)
ana.whatsapp.send_template(
    "919876543210", "invoice_document",
    components=[TemplateComponent.body("INV-001")],
    attachment=FileAttachment.from_path("INV-001.pdf"),
)

# Media
ana.whatsapp.send_media("919876543210", media_type="image", media_url="https://cdn.example.com/p.jpg", caption="New stock")
up = ana.media.upload(FileAttachment.from_path("catalogue.pdf"))
ana.whatsapp.send_media("919876543210", media_upload_id=up.data.mediaUploadId, filename="catalogue.pdf")

# SMS (keyword form, or a SendSmsRequest model)
ana.sms.send(to="919876543210", message="Your OTP is 481920",
             senderId="ANATEC", principalEntityId="1101234567890123456", dltTemplateId="1107161234567890123")
ana.sms.send(SendSmsRequest(message="Store closed today", recipients=["919876543210", "919812345678"]))

# Bulk and campaigns
from anamessaging import SendWhatsAppBulkRequest, RunCampaignRequest
ana.whatsapp.send_bulk(SendWhatsAppBulkRequest(templateName="diwali_offer", recipients=["919876543210"]))
run = ana.campaigns.run(RunCampaignRequest(templateName="diwali_offer", name="Diwali - Delhi"))
detail = ana.campaigns.get(run.data.campaignId)
```

Plain dicts are accepted wherever a request model is:
`ana.whatsapp.send({"to": "...", "text": "..."})`.

## Models

Request models (dataclasses with `.to_dict()`): `SendTextRequest`, `SendTemplateRequest`,
`SendMediaRequest`, `SendWhatsAppBulkRequest`, `RunCampaignRequest`, `SendSmsRequest`,
`TemplateComponent` (`.body(...)`, `.header_text(...)`, `.url_button(...)`), `TemplateParameter`,
`FileAttachment`.

Responses are `ApiResponse` with `success`, `message`, `data` (typed: `MessageDispatchResult`,
`SendSmsResult`, `BulkDispatchSummary`, `RunCampaignResult`, `MediaUploadResult`), `errors`,
`timestamp`, and `raw` (the full JSON).

## Errors

```python
from anamessaging import (AnaMessagingError, SubscriptionInactiveError, QuotaExceededError,
                          RateLimitError, ValidationError, AuthenticationError)

try:
    ana.sms.send(to=to, message=msg)
except SubscriptionInactiveError:
    ...  # renew on anamessaging.com
except QuotaExceededError:
    ...  # monthly allowance used up
except ValidationError as e:
    print(e.message, e.errors)
except AnaMessagingError as e:
    print(e.status, e.code, e)
```

The client is thread-safe. Create it once per process and reuse it, for example in a module-level
cache as the Odoo and ERPNext examples do.

## Develop and publish

```bash
python -m unittest discover -s tests     # runs against a local mock API
python -m build                          # sdist + wheel in dist/
twine upload dist/*                      # PyPI
```
