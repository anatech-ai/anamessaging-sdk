# @anamessaging/sdk

Official ANA Messaging SDK for **Node.js, TypeScript and NestJS**. Send WhatsApp and SMS through the
ANA Messaging API. It has no runtime dependencies and needs Node 18+.

```bash
npm install @anamessaging/sdk
```

You need a **client id and secret** from anamessaging.com (Settings → API Keys) and an **active API
subscription**. Without the subscription, calls throw `SubscriptionInactiveError`.

## Quick start

```ts
import { AnaMessagingClient } from '@anamessaging/sdk';       // ESM / TypeScript
// const { AnaMessagingClient } = require('@anamessaging/sdk'); // CommonJS

const ana = new AnaMessagingClient({
  clientId: process.env.ANAMESSAGING_CLIENT_ID,
  clientSecret: process.env.ANAMESSAGING_CLIENT_SECRET,
  // baseUrl: SANDBOX_BASE_URL,  // https://api-dev.anamessaging.com
});

// Text (inside the 24h customer-service window)
await ana.whatsapp.sendText({ to: '919876543210', text: 'Your order has shipped.' });

// Approved template: "Hi {{1}}, order {{2}} is confirmed."
const res = await ana.whatsapp.sendTemplate({
  to: '919876543210',
  templateName: 'order_confirmation',
  language: 'en',
  components: [{ type: 'body', parameters: [{ type: 'text', text: 'Ravi' }, { type: 'text', text: 'SO-1042' }] }],
  campaignId: 'SO-1042', // your own reference, e.g. ERP document number
});
console.log(res.data?.messageId, res.data?.status); // 'queued'

// Template with a DOCUMENT / IMAGE / VIDEO header: attach the file
import { readFileSync } from 'node:fs';
await ana.whatsapp.sendTemplate({
  to: '919876543210',
  templateName: 'invoice_document',
  components: [{ type: 'body', parameters: [{ type: 'text', text: 'INV-001' }] }],
  attachment: { data: readFileSync('INV-001.pdf'), filename: 'INV-001.pdf' },
});

// Media by URL, or upload once and reuse
await ana.whatsapp.sendMedia({ to: '919876543210', mediaType: 'image', mediaUrl: 'https://cdn.example.com/p.jpg', caption: 'New stock' });
const up = await ana.media.upload({ data: readFileSync('catalogue.pdf'), filename: 'catalogue.pdf' });
await ana.whatsapp.sendMedia({ to: '919876543210', mediaUploadId: up.data!.mediaUploadId, filename: 'catalogue.pdf' });

// SMS: one or many numbers; DLT ids for India
await ana.sms.send({ to: '919876543210', message: 'Your OTP is 481920', senderId: 'ANATEC',
                     principalEntityId: '1101234567890123456', dltTemplateId: '1107161234567890123' });
await ana.sms.send({ recipients: ['919876543210', '919812345678'], message: 'Store closed today' });

// Bulk / campaigns
await ana.whatsapp.sendBulk({ templateName: 'diwali_offer', recipients: ['919876543210', '919812345678'] });
const run = await ana.campaigns.run({ templateName: 'diwali_offer', name: 'Diwali - Delhi' });
const detail = await ana.campaigns.get(run.data!.campaignId); // per-recipient status, skipped + why
```

## Errors

```ts
import { SubscriptionInactiveError, QuotaExceededError, RateLimitError, ValidationError, AnaMessagingError } from '@anamessaging/sdk';

try {
  await ana.sms.send({ to, message });
} catch (e) {
  if (e instanceof SubscriptionInactiveError) { /* renew on anamessaging.com */ }
  else if (e instanceof QuotaExceededError) { /* monthly allowance used up */ }
  else if (e instanceof RateLimitError) { /* e.retryAfterSeconds */ }
  else if (e instanceof ValidationError) { console.error(e.message, e.errors); }
  else if (e instanceof AnaMessagingError) { console.error(e.status, e.code, e.message); }
}
```

| Error | When |
| --- | --- |
| `ValidationError` | 400/413: bad payload, template not approved, attachment problems |
| `AuthenticationError` | 401: wrong client id/secret (`code: 'invalid_client'`) |
| `SubscriptionInactiveError` | 403: no active subscription / API access disabled |
| `PermissionDeniedError` | 403: IP not allowlisted, CSRF |
| `NotFoundError` | 404: unknown template / campaign |
| `RateLimitError` / `QuotaExceededError` | 429: throttled (auto-retried) / quota used up (not retried) |
| `ServerError`, `ConnectionError` | 5xx, network / timeout |

## NestJS

```ts
import { AnaMessagingModule } from '@anamessaging/sdk/nestjs';
import { AnaMessagingClient } from '@anamessaging/sdk';

@Module({
  imports: [
    AnaMessagingModule.forRootAsync({
      inject: [ConfigService],
      useFactory: (c: ConfigService) => ({
        clientId: c.getOrThrow('ANAMESSAGING_CLIENT_ID'),
        clientSecret: c.getOrThrow('ANAMESSAGING_CLIENT_SECRET'),
      }),
    }),
  ],
})
export class AppModule {}

@Injectable()
export class Notifications {
  constructor(private readonly ana: AnaMessagingClient) {} // or @InjectAnaMessaging()
}
```

The module is global by default. `forRoot(config)` takes a static config. A full app is in
[`../examples/nestjs-app`](../examples/nestjs-app).

## Options

| Option | Default | |
| --- | --- | --- |
| `clientId`, `clientSecret` | env `ANAMESSAGING_CLIENT_ID/SECRET` | issued on anamessaging.com |
| `scope` | env `ANAMESSAGING_SCOPE` | optional |
| `accessToken`, `csrfToken` | – | use a token you already have (not refreshed) |
| `baseUrl` | `https://api.anamessaging.com` | env `ANAMESSAGING_BASE_URL` |
| `timeoutMs` | 30000 | per request |
| `maxRetries` | 2 | 429 throttle / 503 only |
| `fetch` | global `fetch` | custom fetch, e.g. one that goes through a proxy |

## Develop and publish

```bash
npm ci
npm test                         # builds, then runs node:test suites against a local mock API
npm pack                         # inspect the tarball
npm publish --access public      # @anamessaging scope on npmjs.com
```
