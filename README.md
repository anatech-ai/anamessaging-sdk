# ANA Messaging SDKs

Support --> kumaran@anatech.ai / 7904058479

Official client libraries for the **ANA Messaging API**. Send WhatsApp messages (text, templates,
media, bulk, campaigns) and SMS from any Node.js / TypeScript / NestJS, Python or Java application:
ERPs, e-commerce stores, desktop tools and back-office systems.

This folder is **independent** of the rest of the repository. Each SDK talks only to the public
REST API (`https://api.anamessaging.com/api/v1/anamsg`), has its own build, and is published on
its own.

| SDK | Package | Install | Runtime deps |
| --- | --- | --- | --- |
| [Node.js / TypeScript / NestJS](node/) | npm `@anamessaging/sdk` | `npm i @anamessaging/sdk` | none (built-in `fetch`, Node 18+) |
| [Python](python/) | PyPI `anamessaging` | `pip install anamessaging` | none (standard library, 3.8+) |
| [Java](java/) | Maven `com.anamessaging:anamessaging-sdk` | see [java/README](java/README.md) | Jackson (or the self-contained `-all.jar`) |

## Before you start: credentials and subscription

1. Subscribe to an **ANA Messaging API plan** on [anamessaging.com](https://anamessaging.com).
2. Open **Settings → API Keys** and generate credentials. You get a **client id** and a
   **client secret**. The secret is shown once, so store it in a secret manager or environment variables.
3. Optional: set the IP allowlist for your servers on the same page.

The SDKs exchange the client id and secret for an OAuth bearer token plus a CSRF token
(`POST /oauth2`), cache it, refresh it before it expires, and send both headers on every call.
You never handle tokens yourself.

> **No active subscription, no messages.** The API checks the subscription on every call. If it
> is missing or expired, or API access is disabled for your organisation, every SDK raises
> `SubscriptionInactiveError` (Java: `SubscriptionInactiveException`). If the monthly allowance is
> used up, it raises `QuotaExceededError`. Credentials alone do not let you send.

All SDKs read the same environment variables:

| Variable | Meaning |
| --- | --- |
| `ANAMESSAGING_CLIENT_ID` | client id |
| `ANAMESSAGING_CLIENT_SECRET` | client secret |
| `ANAMESSAGING_BASE_URL` | default `https://api.anamessaging.com`; sandbox `https://api-dev.anamessaging.com` |
| `ANAMESSAGING_SCOPE` | optional, `<client id>/<org slug>-<org id>` |

## Same API in every language

| Operation | Node | Python | Java |
| --- | --- | --- | --- |
| WhatsApp text | `whatsapp.sendText({to, text})` | `whatsapp.send_text(to, text)` | `whatsapp().sendText(to, text)` |
| WhatsApp template | `whatsapp.sendTemplate({...})` | `whatsapp.send_template(...)` | `whatsapp().send(SendWhatsAppRequest.template(...))` |
| Template + PDF/image header | `attachment: {data, filename}` | `attachment=FileAttachment(...)` | `send(request, new FileAttachment(...))` |
| WhatsApp media | `whatsapp.sendMedia({...})` | `whatsapp.send_media(...)` | `SendWhatsAppRequest.mediaUrl(...)` |
| Upload media once | `media.upload(file)` | `media.upload(file)` | `media().upload(file, type)` |
| Bulk template | `whatsapp.sendBulk({...})` | `whatsapp.send_bulk(...)` | `whatsapp().sendBulk(...)` |
| Recorded campaign | `campaigns.run / list / get` | `campaigns.run / list / get` | `campaigns().run / list / get` |
| SMS (one or many, DLT) | `sms.send({...})` | `sms.send(to=..., message=...)` | `sms().send(SendSmsRequest...)` |
| Check credentials | `authenticate()` | `authenticate()` | `authenticate()` |

Every SDK also provides:

- Typed request/response models that match the API DTOs (`SendWhatsAppRequest`,
  `SendSmsRequest`, `MessageDispatchResult`, `ApiResponse<T>`, ...).
- Typed errors: `ValidationError` (400), `AuthenticationError` (401), `PermissionDeniedError` (403),
  `SubscriptionInactiveError` (403), `NotFoundError` (404), `RateLimitError` / `QuotaExceededError`
  (429), `ServerError` (5xx), `ConnectionError`.
- Automatic token refresh, and one retry with a new token on a 401.
- Retries on the per-second throttle (429 `RATE_LIMIT_PER_SECOND`, honouring `Retry-After`) and on
  503. These are refused before anything is queued, so retrying cannot send duplicates. Quota
  errors and other failures are never retried.
- Thread / concurrency safety: create one client and share it.

A successful call means the message was **queued** (HTTP 202). It does not mean it was delivered.
Track delivery through your webhook, or `campaigns.get(id)` for campaigns.

## Integrations

| Platform | Example | SDK |
| --- | --- | --- |
| **Odoo** (16/17) | [examples/odoo](examples/odoo/): addon with settings, order/invoice WhatsApp, SMS reminders | Python |
| **ERPNext / Frappe** | [examples/erpnext](examples/erpnext/): app with `doc_events` on Sales Order / Invoice / Delivery Note, whitelisted API | Python |
| **Shopify** | [examples/ecommerce-webhooks](examples/ecommerce-webhooks/): HMAC-verified `orders/*` webhooks | Node |
| **WooCommerce** | same server, `/webhooks/woocommerce` | Node |
| Magento, BigCommerce, PrestaShop, custom stores | same server, `/webhooks/generic` with a shared token | Node |
| **NestJS** apps | [examples/nestjs-app](examples/nestjs-app/): `AnaMessagingModule.forRootAsync` | Node |
| Desktop (Swing / JavaFX / POS / billing) | [examples/desktop-java](examples/desktop-java/) | Java `-all.jar` |
| Desktop (Electron) | use the Node SDK in the **main** process (never the renderer) | Node |
| SAP, Dynamics, Tally connectors, Spring Boot | Java SDK as a Maven/Gradle dependency | Java |
| Django / Flask / FastAPI / scripts / cron | `pip install anamessaging` | Python |

> **Never ship the client secret inside a browser app, mobile app or a desktop installer you
> distribute to end users.** Keep it on a server you control (or the customer's own server for
> on-premise ERPs) and have the client call that server.

## Build, test and publish

```bash
# Node (11 tests)
cd node && npm ci && npm test
npm publish --access public            # needs an npm token for the @anamessaging scope

# Python (9 tests)
cd python && python -m unittest discover -s tests
python -m build && twine upload dist/*  # needs a PyPI token

# Java (9 tests)
cd java && mvn package                 # jar, sources, javadoc, and a self-contained -all.jar
mvn deploy                             # needs a <distributionManagement> repository; see java/README
```

The [`.github/workflows/sdk-publish.yml`](../.github/workflows/sdk-publish.yml) workflow runs all
three test suites on every change under `anamessaging-sdk/`. On a `sdk-v*` tag it publishes to
npm, PyPI and GitHub Packages (Maven).

## Versioning

All three SDKs share one version number (`1.0.0`) and follow semver. A new optional API field is
a minor release. A removed or renamed field is a major release. Response models ignore unknown
fields, so a newer API never breaks an older SDK.
