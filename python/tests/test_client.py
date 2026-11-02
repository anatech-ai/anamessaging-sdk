import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import anamessaging as ana


class MockApi:
    """Stand-in for the ANA Messaging API."""

    def __init__(self, handlers=None):
        self.calls, self.token_count, self.handlers = [], 0, handlers or {}
        api = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *a):
                pass

            def _handle(self):
                raw = self.rfile.read(int(self.headers.get("Content-Length") or 0))
                body = json.loads(raw) if "json" in (self.headers.get("Content-Type") or "") else raw
                call = {"method": self.command, "path": self.path, "headers": dict(self.headers), "body": body}
                api.calls.append(call)

                def send(status, payload, headers=None):
                    self.send_response(status)
                    self.send_header("Content-Type", "application/json")
                    for k, v in (headers or {}).items():
                        self.send_header(k, v)
                    out = json.dumps(payload).encode()
                    self.send_header("Content-Length", str(len(out)))
                    self.end_headers()
                    self.wfile.write(out)

                key = "%s %s" % (self.command, self.path.split("?")[0])
                if key in api.handlers:
                    return api.handlers[key](call, send)
                if key == "POST /api/v1/anamsg/oauth2":
                    if body["client_secret"] != "secret":
                        return send(401, {"error": "invalid_client", "error_description": "Client authentication failed"})
                    api.token_count += 1
                    n = api.token_count
                    return send(200, {"access_token": "token-%d" % n, "token_type": "Bearer", "expires_in": 3600,
                                      "csrf_token": "csrf-%d" % n})
                n = api.token_count
                if self.headers.get("Authorization") != "Bearer token-%d" % n or self.headers.get("x-csrf-token") != "csrf-%d" % n:
                    return send(401, {"success": False, "message": "Bearer token is invalid, expired or revoked"})
                send(202, {"success": True, "message": "queued",
                           "data": {"messageId": 7, "to": "919876543210", "status": "queued",
                                    "total": 1, "messages": [{"messageId": 7, "status": "queued"}]}})

            do_GET = do_POST = _handle

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.base_url = "http://127.0.0.1:%d" % self.server.server_address[1]

    def client(self, **kw):
        kw.setdefault("client_secret", "secret")
        return ana.AnaMessagingClient("cid", base_url=self.base_url, **kw)

    def close(self):
        self.server.shutdown()
        self.server.server_close()


class ClientTest(unittest.TestCase):
    def setUp(self):
        self.api = None

    def tearDown(self):
        if self.api:
            self.api.close()

    def mock(self, handlers=None):
        self.api = MockApi(handlers)
        return self.api

    def test_requires_credentials(self):
        with self.assertRaises(ValueError):
            ana.AnaMessagingClient(base_url="http://x")

    def test_text_and_sms_share_token(self):
        api = self.mock()
        c = api.client()
        r = c.whatsapp.send_text("919876543210", "Hi")
        self.assertIsInstance(r.data, ana.MessageDispatchResult)
        self.assertEqual(r.data.status, "queued")
        s = c.sms.send(to="919876543210", message="OTP 1", dltTemplateId="11")
        self.assertEqual(s.data.messages[0].messageId, 7)
        self.assertEqual(len([x for x in api.calls if x["path"].endswith("/oauth2")]), 1)
        self.assertEqual(api.calls[1]["body"], {"to": "919876543210", "text": "Hi"})
        self.assertEqual(api.calls[2]["body"], {"message": "OTP 1", "to": "919876543210", "dltTemplateId": "11"})

    def test_template_components_and_multipart(self):
        api = self.mock()
        c = api.client()
        c.whatsapp.send_template("91987", "order_shipped", components=[ana.TemplateComponent.body("Ravi", "SO-1")])
        self.assertEqual(api.calls[-1]["body"]["components"],
                         [{"type": "body", "parameters": [{"type": "text", "text": "Ravi"}, {"type": "text", "text": "SO-1"}]}])
        self.assertEqual(api.calls[-1]["body"]["language"], "en")
        c.whatsapp.send_template("91987", "invoice", attachment=ana.FileAttachment(b"%PDF-1.4", "inv.pdf"),
                                 components=[{"type": "body", "parameters": []}])
        last = api.calls[-1]
        self.assertIn("multipart/form-data", last["headers"]["Content-Type"])
        self.assertIn(b'filename="inv.pdf"', last["body"])
        self.assertIn(b"Content-Type: application/pdf", last["body"])
        self.assertIn(b'[{"type": "body", "parameters": []}]', last["body"])

    def test_refresh_on_401(self):
        api = self.mock()
        c = api.client()
        c.whatsapp.send_text("91987", "a")
        api.token_count += 1  # server revokes
        self.assertTrue(c.whatsapp.send_text("91987", "b").success)
        self.assertEqual(len([x for x in api.calls if x["path"].endswith("/oauth2")]), 2)

    def test_bad_secret(self):
        api = self.mock()
        with self.assertRaises(ana.AuthenticationError) as ctx:
            api.client(client_secret="nope").sms.send(to="91987", message="x")
        self.assertEqual(ctx.exception.code, "invalid_client")

    def test_subscription_inactive(self):
        api = self.mock({"POST /api/v1/anamsg/sms/sent": lambda c, send: send(
            403, {"success": False, "message": "No active subscription found", "code": "NO_SUBSCRIPTION"})})
        with self.assertRaises(ana.SubscriptionInactiveError) as ctx:
            api.client().sms.send(to="91987", message="x")
        self.assertIsInstance(ctx.exception, ana.PermissionDeniedError)

    def test_throttle_retry_and_quota(self):
        n = {"c": 0}

        def wa(call, send):
            n["c"] += 1
            if n["c"] == 1:
                return send(429, {"success": False, "code": "RATE_LIMIT_PER_SECOND", "message": "slow"}, {"Retry-After": "0"})
            send(202, {"success": True, "data": {"status": "queued"}})

        api = self.mock({
            "POST /api/v1/anamsg/send/whatsapp": wa,
            "POST /api/v1/anamsg/sms/sent": lambda c, s: s(429, {"success": False, "code": "QUOTA_EXHAUSTED", "message": "q"}),
        })
        c = api.client()
        self.assertEqual(c.whatsapp.send_text("91987", "x").data.status, "queued")
        self.assertEqual(n["c"], 2)
        with self.assertRaises(ana.QuotaExceededError):
            c.sms.send(to="91987", message="x")

    def test_validation_errors(self):
        api = self.mock({"POST /api/v1/anamsg/send/whatsapp": lambda c, s: s(
            400, {"success": False, "message": "Request validation failed", "errors": ["to too short"]})})
        with self.assertRaises(ana.ValidationError) as ctx:
            api.client().whatsapp.send_text("1", "x")
        self.assertEqual(ctx.exception.errors, ["to too short"])

    def test_health_campaigns_preissued(self):
        api = self.mock({"GET /api/v1/anamsg/health": lambda c, s: s(200, {"status": "ok"})})
        c = api.client()
        self.assertEqual(c.health()["status"], "ok")
        self.assertNotIn("Authorization", api.calls[0]["headers"])
        c.campaigns.list(limit=10)
        self.assertEqual(api.calls[-1]["path"], "/api/v1/anamsg/campaigns?limit=10")
        pre = ana.AnaMessagingClient(base_url=api.base_url, access_token="tok", csrf_token="cs")
        with self.assertRaises(ana.AuthenticationError):
            pre.campaigns.get(5)
        self.assertEqual(api.calls[-1]["headers"]["Authorization"], "Bearer tok")
        self.assertEqual(api.calls[-1]["path"], "/api/v1/anamsg/campaigns/5")


if __name__ == "__main__":
    unittest.main()
