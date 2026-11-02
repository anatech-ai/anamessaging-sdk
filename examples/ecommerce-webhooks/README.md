# Shopify, WooCommerce and other stores

A small Node server that turns store order webhooks into WhatsApp template messages through
`@anamessaging/sdk`. If a WhatsApp send is rejected (for example, the template can't be used
for that number), it falls back to SMS.

| Route | Platform | Verification |
| --- | --- | --- |
| `POST /webhooks/shopify` | Shopify `orders/create`, `orders/fulfilled`, `orders/cancelled` | `X-Shopify-Hmac-Sha256` (HMAC-SHA256 of the raw body) |
| `POST /webhooks/woocommerce` | WooCommerce "Order created" / "Order updated" | `X-WC-Webhook-Signature` |
| `POST /webhooks/generic` | Magento, BigCommerce, PrestaShop, Wix, custom carts | `X-Webhook-Token` shared secret |

The generic body is
`{ "event": "order_created|order_fulfilled|order_cancelled", "orderId", "customerName", "phone", "total", "currency", "trackingUrl" }`.

## Run

```bash
cp .env.example .env    # fill in the ANA credentials and webhook secrets
npm install
node --env-file=.env server.js
```

Then register the webhooks:

- **Shopify**: Admin → Settings → Notifications → Webhooks (or the Admin API `webhookSubscriptionCreate`
  from your Shopify app). Use JSON format with the topics above, pointing at `https://your-host/webhooks/shopify`.
- **WooCommerce**: Settings → Advanced → Webhooks → Add. Topic "Order updated", secret =
  `WOOCOMMERCE_WEBHOOK_SECRET`.

Edit `TEMPLATES` in `server.js` to match your approved template names and placeholders.
A public Shopify app built with the Shopify CLI (Remix) can import `@anamessaging/sdk` directly in
its webhook handlers the same way. Keep the ANA credentials on the app server, never in a theme
or app extension.
