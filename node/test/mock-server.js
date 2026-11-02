// Minimal stand-in for the ANA Messaging API, enough to exercise the SDK end to end.
const http = require('http');

function startMockServer(handlers = {}) {
  const calls = [];
  let tokenCount = 0;
  const server = http.createServer((req, res) => {
    let raw = '';
    req.on('data', (c) => (raw += c));
    req.on('end', () => {
      let body = raw;
      if ((req.headers['content-type'] || '').includes('application/json')) body = JSON.parse(raw);
      const call = { method: req.method, path: req.url, headers: req.headers, body };
      calls.push(call);
      const send = (status, payload, headers = {}) => {
        res.writeHead(status, { 'Content-Type': 'application/json', ...headers });
        res.end(JSON.stringify(payload));
      };
      const key = `${req.method} ${req.url.split('?')[0]}`;
      if (handlers[key]) return handlers[key](call, send, calls);
      if (key === 'POST /api/v1/anamsg/oauth2') {
        if (body.client_secret !== 'secret') return send(401, { error: 'invalid_client', error_description: 'Client authentication failed' });
        tokenCount++;
        return send(200, {
          access_token: `token-${tokenCount}`, token_type: 'Bearer', expires_in: 3600,
          csrf_token: `csrf-${tokenCount}`, expires_at: new Date(Date.now() + 3600e3).toISOString(),
        });
      }
      if (req.headers.authorization !== `Bearer token-${tokenCount}` || req.headers['x-csrf-token'] !== `csrf-${tokenCount}`) {
        return send(401, { success: false, message: 'Bearer token is invalid, expired or revoked' });
      }
      send(202, { success: true, message: 'queued', data: { messageId: 1, to: '919876543210', status: 'queued' }, timestamp: new Date().toISOString() });
    });
  });
  return new Promise((resolve) =>
    server.listen(0, '127.0.0.1', () =>
      resolve({ server, calls, baseUrl: `http://127.0.0.1:${server.address().port}`, revoke: () => tokenCount++ }),
    ),
  );
}

module.exports = { startMockServer };
