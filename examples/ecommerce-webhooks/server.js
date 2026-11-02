// Store order events -> WhatsApp / SMS through ANA Messaging.
//
//   POST /webhooks/shopify       Shopify webhooks (orders/create, orders/fulfilled, orders/cancelled)
//   POST /webhooks/woocommerce   WooCommerce webhooks (Order created / updated)
//   POST /webhooks/generic       Any platform: { event, orderId, customerName, phone, total, currency, trackingUrl }
//
// Every request's signature is verified before anything is sent. Uses only the SDK and Node built-ins.
import { createHmac, timingSafeEqual } from 'node:crypto';
import { createServer } from 'node:http';
import { AnaMessagingClient, AnaMessagingError, SubscriptionInactiveError, ValidationError } from '@anamessaging/sdk';

const ana = new AnaMessagingClient(); // ANAMESSAGING_CLIENT_ID / _SECRET from the environment
const CC = process.env.DEFAULT_COUNTRY_CODE || '91';

// Your approved WhatsApp templates. Body placeholders are filled in order.
const TEMPLATES = {
  order_created: 'order_confirmation', // "Hi {{1}}, thanks for order {{2}} of {{3}}."
  order_fulfilled: 'order_shipped', // "Hi {{1}}, order {{2}} has shipped. Track: {{3}}"
  order_cancelled: 'order_cancelled', // "Hi {{1}}, order {{2}} was cancelled."
};

const phone = (raw) => {
  const d = String(raw || '').replace(/\D/g, '');
  return d.length === 10 ? CC + d : d;
};

const safeEqual = (a, b) => {
  const x = Buffer.from(a || '');
  const y = Buffer.from(b || '');
  return x.length === y.length && timingSafeEqual(x, y);
};
const hmacB64 = (secret, body) => createHmac('sha256', secret).update(body).digest('base64');

/** One order event, whatever the platform. */
async function notify({ event, orderId, customerName, phone: to, total, currency, trackingUrl }) {
  const template = TEMPLATES[event];
  if (!template || !to) return { skipped: true };
  const params =
    event === 'order_created' ? [customerName, orderId, `${currency} ${total}`]
    : event === 'order_fulfilled' ? [customerName, orderId, trackingUrl || '-']
    : [customerName, orderId];
  try {
    return await ana.whatsapp.sendTemplate({
      to: phone(to),
      templateName: template,
      components: [{ type: 'body', parameters: params.map((text) => ({ type: 'text', text: String(text ?? '') })) }],
      campaignId: String(orderId),
    });
  } catch (error) {
    // Template not usable for this customer (e.g. a number without WhatsApp): fall back to SMS.
    if (error instanceof ValidationError) {
      return ana.sms.send({ to: phone(to), message: `Hi ${customerName}, update on order ${orderId}: ${event.replace('order_', '')}.` });
    }
    throw error;
  }
}

// ── Platform adapters ──

function fromShopify(topic, o) {
  const event = { 'orders/create': 'order_created', 'orders/fulfilled': 'order_fulfilled', 'orders/cancelled': 'order_cancelled' }[topic];
  const addr = o.shipping_address || o.billing_address || {};
  return {
    event,
    orderId: o.name || o.order_number || o.id,
    customerName: o.customer?.first_name || addr.first_name || 'there',
    phone: o.phone || o.customer?.phone || addr.phone,
    total: o.total_price,
    currency: o.currency,
    trackingUrl: o.fulfillments?.[0]?.tracking_url,
  };
}

function fromWooCommerce(o) {
  const event = { processing: 'order_created', 'on-hold': 'order_created', completed: 'order_fulfilled', cancelled: 'order_cancelled' }[o.status];
  return {
    event,
    orderId: o.number || o.id,
    customerName: o.billing?.first_name || 'there',
    phone: o.billing?.phone || o.shipping?.phone,
    total: o.total,
    currency: o.currency,
  };
}

// ── HTTP ──

const readBody = (req) =>
  new Promise((resolve, reject) => {
    const chunks = [];
    req.on('data', (c) => chunks.push(c));
    req.on('end', () => resolve(Buffer.concat(chunks)));
    req.on('error', reject);
  });

createServer(async (req, res) => {
  const reply = (status, body) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(body));
  };
  if (req.method !== 'POST') return reply(405, { error: 'POST only' });
  const raw = await readBody(req);

  let order;
  if (req.url === '/webhooks/shopify') {
    if (!safeEqual(hmacB64(process.env.SHOPIFY_WEBHOOK_SECRET || '', raw), req.headers['x-shopify-hmac-sha256'])) {
      return reply(401, { error: 'bad signature' });
    }
    order = fromShopify(req.headers['x-shopify-topic'], JSON.parse(raw));
  } else if (req.url === '/webhooks/woocommerce') {
    if (!req.headers['x-wc-webhook-signature']) return reply(200, { ok: true }); // WooCommerce's ping on save
    if (!safeEqual(hmacB64(process.env.WOOCOMMERCE_WEBHOOK_SECRET || '', raw), req.headers['x-wc-webhook-signature'])) {
      return reply(401, { error: 'bad signature' });
    }
    order = fromWooCommerce(JSON.parse(raw));
  } else if (req.url === '/webhooks/generic') {
    if (!process.env.GENERIC_WEBHOOK_TOKEN || !safeEqual(req.headers['x-webhook-token'], process.env.GENERIC_WEBHOOK_TOKEN)) {
      return reply(401, { error: 'bad token' });
    }
    order = JSON.parse(raw);
  } else {
    return reply(404, { error: 'not found' });
  }

  try {
    const result = await notify(order);
    reply(200, { ok: true, result });
  } catch (error) {
    if (error instanceof SubscriptionInactiveError) console.error('ANA Messaging subscription inactive - renew on anamessaging.com');
    else console.error('ANA Messaging error', error instanceof AnaMessagingError ? `${error.status} ${error.message}` : error);
    // 200 so the store does not retry forever on a permanent error; 5xx would make it retry.
    reply(error instanceof AnaMessagingError && error.status >= 500 ? 502 : 200, { ok: false, error: error.message });
  }
}).listen(process.env.PORT || 8080, () => console.log(`ANA Messaging store webhooks on :${process.env.PORT || 8080}`));
