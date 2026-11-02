const test = require('node:test');
const assert = require('node:assert');
const { startMockServer } = require('./mock-server');
const sdk = require('../dist');

const make = (baseUrl, extra = {}) => new sdk.AnaMessagingClient({ baseUrl, clientId: 'cid', clientSecret: 'secret', ...extra });

test('requires credentials', () => {
  assert.throws(() => new sdk.AnaMessagingClient({ baseUrl: 'http://x' }), /clientId and clientSecret/);
});

test('sends WhatsApp text with bearer + csrf, reusing one token', async () => {
  const m = await startMockServer();
  const ana = make(m.baseUrl);
  const r = await ana.whatsapp.sendText({ to: '919876543210', text: 'Hi' });
  await ana.sms.send({ to: '919876543210', message: 'OTP 1' });
  assert.equal(r.data.status, 'queued');
  const posts = m.calls.filter((c) => c.path === '/api/v1/anamsg/oauth2');
  assert.equal(posts.length, 1);
  assert.deepEqual(posts[0].body, { grant_type: 'client_credentials', client_id: 'cid', client_secret: 'secret' });
  const wa = m.calls.find((c) => c.path === '/api/v1/anamsg/send/whatsapp');
  assert.deepEqual(wa.body, { to: '919876543210', text: 'Hi' });
  assert.equal(m.calls.find((c) => c.path === '/api/v1/anamsg/sms/sent').body.message, 'OTP 1');
  m.server.close();
});

test('template defaults language and sends attachment as multipart', async () => {
  const m = await startMockServer();
  const ana = make(m.baseUrl);
  await ana.whatsapp.sendTemplate({ to: '91987', templateName: 'order_shipped', components: [{ type: 'body', parameters: [{ type: 'text', text: 'A1' }] }] });
  assert.equal(m.calls.at(-1).body.language, 'en');
  await ana.whatsapp.sendTemplate({
    to: '91987', templateName: 'invoice', attachment: { data: Buffer.from('%PDF-1.4'), filename: 'inv.pdf' },
    components: [{ type: 'body', parameters: [] }],
  });
  const last = m.calls.at(-1);
  assert.match(last.headers['content-type'], /multipart\/form-data/);
  assert.match(last.body, /name="file"; filename="inv.pdf"/);
  assert.match(last.body, /Content-Type: application\/pdf/);
  assert.match(last.body, /\[\{"type":"body","parameters":\[\]\}\]/);
  m.server.close();
});

test('refreshes token once on 401 and retries', async () => {
  const m = await startMockServer();
  const ana = make(m.baseUrl);
  await ana.whatsapp.sendText({ to: '91987', text: 'a' });
  m.revoke();
  const r = await ana.whatsapp.sendText({ to: '91987', text: 'b' });
  assert.equal(r.success, true);
  assert.equal(m.calls.filter((c) => c.path.endsWith('/oauth2')).length, 2);
  m.server.close();
});

test('bad secret -> AuthenticationError', async () => {
  const m = await startMockServer();
  const ana = make(m.baseUrl, { clientSecret: 'nope' });
  await assert.rejects(ana.sms.send({ to: '91987', message: 'x' }), (e) => e instanceof sdk.AuthenticationError && e.code === 'invalid_client');
  m.server.close();
});

test('inactive subscription -> SubscriptionInactiveError', async () => {
  const m = await startMockServer({
    'POST /api/v1/anamsg/sms/sent': (c, send) => send(403, { success: false, message: 'Subscription has expired', code: 'SUBSCRIPTION_EXPIRED' }),
  });
  await assert.rejects(make(m.baseUrl).sms.send({ to: '91987', message: 'x' }), (e) =>
    e instanceof sdk.SubscriptionInactiveError && e instanceof sdk.PermissionDeniedError && e.status === 403);
  m.server.close();
});

test('retries per-second throttle using Retry-After, not quota exhaustion', async () => {
  let n = 0;
  const m = await startMockServer({
    'POST /api/v1/anamsg/send/whatsapp': (c, send) =>
      ++n === 1 ? send(429, { success: false, message: 'slow', code: 'RATE_LIMIT_PER_SECOND' }, { 'Retry-After': '0' })
        : send(202, { success: true, data: { status: 'queued' } }),
    'POST /api/v1/anamsg/sms/sent': (c, send) => send(429, { success: false, message: 'Monthly request quota exhausted', code: 'QUOTA_EXHAUSTED' }),
  });
  const ana = make(m.baseUrl);
  assert.equal((await ana.whatsapp.sendText({ to: '91987', text: 'x' })).data.status, 'queued');
  assert.equal(n, 2);
  await assert.rejects(ana.sms.send({ to: '91987', message: 'x' }), sdk.QuotaExceededError);
  assert.equal(m.calls.filter((c) => c.path.endsWith('/sms/sent')).length, 1);
  m.server.close();
});

test('validation error carries field errors', async () => {
  const m = await startMockServer({
    'POST /api/v1/anamsg/send/whatsapp': (c, send) => send(400, { success: false, message: 'Request validation failed', errors: ['to must be longer than or equal to 7 characters'] }),
  });
  await assert.rejects(make(m.baseUrl).whatsapp.sendText({ to: '1', text: 'x' }), (e) => e instanceof sdk.ValidationError && e.errors.length === 1);
  m.server.close();
});

test('campaigns list/get, health without auth, pre-issued token', async () => {
  const m = await startMockServer({
    'GET /api/v1/anamsg/health': (c, send) => send(200, { status: 'ok' }),
    'GET /api/v1/anamsg/campaigns': (c, send) => send(200, { success: true, data: { total: 0, campaigns: [], q: c.path } }),
  });
  const ana = make(m.baseUrl);
  assert.equal((await ana.health()).status, 'ok');
  assert.equal(m.calls[0].headers.authorization, undefined);
  assert.equal((await ana.campaigns.list({ limit: 10 })).data.q, '/api/v1/anamsg/campaigns?limit=10');
  const pre = new sdk.AnaMessagingClient({ baseUrl: m.baseUrl, accessToken: 'tok', csrfToken: 'cs' });
  await pre.campaigns.get(5).catch(() => {});
  const g = m.calls.at(-1);
  assert.equal(g.path, '/api/v1/anamsg/campaigns/5');
  assert.equal(g.headers.authorization, 'Bearer tok');
  m.server.close();
});
