package com.anamessaging.sdk;

import com.anamessaging.sdk.exception.*;
import com.anamessaging.sdk.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AnaMessagingClientTest {
    interface Handler { void handle(Call call, HttpExchange ex) throws IOException; }

    static final class Call {
        String method, path, body, auth, csrf, contentType;
    }

    final ObjectMapper mapper = new ObjectMapper();
    final List<Call> calls = new CopyOnWriteArrayList<>();
    final Map<String, Handler> handlers = new ConcurrentHashMap<>();
    final AtomicInteger tokens = new AtomicInteger();
    HttpServer server;
    String baseUrl;

    static void reply(HttpExchange ex, int status, String json, String... headers) throws IOException {
        byte[] b = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        for (int i = 0; i + 1 < headers.length; i += 2) ex.getResponseHeaders().add(headers[i], headers[i + 1]);
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            Call c = new Call();
            c.method = ex.getRequestMethod();
            c.path = ex.getRequestURI().toString();
            c.body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            c.auth = ex.getRequestHeaders().getFirst("Authorization");
            c.csrf = ex.getRequestHeaders().getFirst("x-csrf-token");
            c.contentType = ex.getRequestHeaders().getFirst("Content-Type");
            calls.add(c);
            String key = c.method + " " + ex.getRequestURI().getPath();
            if (handlers.containsKey(key)) { handlers.get(key).handle(c, ex); return; }
            if (key.equals("POST /api/v1/anamsg/oauth2")) {
                JsonNode b = mapper.readTree(c.body);
                if (!"secret".equals(b.path("client_secret").asText())) {
                    reply(ex, 401, "{\"error\":\"invalid_client\",\"error_description\":\"Client authentication failed\"}");
                    return;
                }
                int n = tokens.incrementAndGet();
                reply(ex, 200, "{\"access_token\":\"token-" + n + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,\"csrf_token\":\"csrf-" + n + "\"}");
                return;
            }
            int n = tokens.get();
            if (!("Bearer token-" + n).equals(c.auth) || !("csrf-" + n).equals(c.csrf)) {
                reply(ex, 401, "{\"success\":false,\"message\":\"Bearer token is invalid, expired or revoked\"}");
                return;
            }
            reply(ex, 202, "{\"success\":true,\"message\":\"queued\",\"data\":{\"messageId\":7,\"to\":\"919876543210\",\"status\":\"queued\",\"total\":1,\"messages\":[{\"messageId\":7,\"status\":\"queued\"}]}}");
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() { server.stop(0); }

    AnaMessagingClient client(String secret) {
        return AnaMessagingClient.builder().baseUrl(baseUrl).clientId("cid").clientSecret(secret).build();
    }

    long count(String suffix) { return calls.stream().filter(c -> c.path.endsWith(suffix)).count(); }

    @Test
    void requiresCredentials() {
        assertThrows(IllegalStateException.class, () -> AnaMessagingClient.builder().clientId(null).clientSecret(null).baseUrl("http://x").build());
    }

    @Test
    void sendsTextAndSmsWithOneToken() throws Exception {
        AnaMessagingClient ana = client("secret");
        ApiResponse<MessageDispatchResult> r = ana.whatsapp().sendText("919876543210", "Hi");
        assertTrue(r.success);
        assertTrue(r.data.isQueued());
        ApiResponse<SendSmsResult> s = ana.sms().send(SendSmsRequest.to("919876543210", "OTP 1").dlt("1101", "1107"));
        assertEquals(1, s.data.total);
        assertEquals(1, count("/oauth2"));
        assertEquals(mapper.readTree("{\"to\":\"919876543210\",\"text\":\"Hi\"}"), mapper.readTree(calls.get(1).body));
        JsonNode sms = mapper.readTree(calls.get(2).body);
        assertEquals("1107", sms.path("dltTemplateId").asText());
        assertFalse(sms.has("recipients"));
    }

    @Test
    void templateJsonAndMultipart() throws Exception {
        AnaMessagingClient ana = client("secret");
        ana.whatsapp().sendTemplate("91987", "order_shipped", null, TemplateComponent.body("Ravi", "SO-1"));
        JsonNode t = mapper.readTree(calls.get(calls.size() - 1).body);
        assertEquals("en", t.path("language").asText());
        assertEquals("Ravi", t.path("components").get(0).path("parameters").get(0).path("text").asText());

        ana.whatsapp().send(SendWhatsAppRequest.template("91987", "invoice").components(TemplateComponent.body("A")),
                new FileAttachment("%PDF-1.4".getBytes(), "inv.pdf"));
        Call last = calls.get(calls.size() - 1);
        assertTrue(last.contentType.startsWith("multipart/form-data; boundary="));
        assertTrue(last.body.contains("filename=\"inv.pdf\""));
        assertTrue(last.body.contains("Content-Type: application/pdf"));
        assertTrue(last.body.contains("name=\"templateName\"\r\n\r\ninvoice"));
        assertTrue(last.body.contains("[{\"type\":\"body\",\"parameters\":[{\"type\":\"text\",\"text\":\"A\"}]}]"));
    }

    @Test
    void refreshesTokenOn401() {
        AnaMessagingClient ana = client("secret");
        ana.whatsapp().sendText("91987", "a");
        tokens.incrementAndGet(); // server-side revocation
        assertTrue(ana.whatsapp().sendText("91987", "b").success);
        assertEquals(2, count("/oauth2"));
    }

    @Test
    void badSecret() {
        AuthenticationException e = assertThrows(AuthenticationException.class, () -> client("nope").sms().send("91987", "x"));
        assertEquals("invalid_client", e.getCode());
    }

    @Test
    void subscriptionInactive() {
        handlers.put("POST /api/v1/anamsg/sms/sent", (c, ex) -> reply(ex, 403, "{\"success\":false,\"message\":\"Subscription has expired\",\"code\":\"SUBSCRIPTION_EXPIRED\"}"));
        SubscriptionInactiveException e = assertThrows(SubscriptionInactiveException.class, () -> client("secret").sms().send("91987", "x"));
        assertEquals(403, e.getStatus());
        assertTrue(e instanceof PermissionDeniedException);
    }

    @Test
    void retriesThrottleButNotQuota() {
        AtomicInteger n = new AtomicInteger();
        handlers.put("POST /api/v1/anamsg/send/whatsapp", (c, ex) -> {
            if (n.incrementAndGet() == 1) reply(ex, 429, "{\"success\":false,\"code\":\"RATE_LIMIT_PER_SECOND\",\"message\":\"slow\"}", "Retry-After", "0");
            else reply(ex, 202, "{\"success\":true,\"data\":{\"status\":\"queued\"}}");
        });
        handlers.put("POST /api/v1/anamsg/sms/sent", (c, ex) -> reply(ex, 429, "{\"success\":false,\"code\":\"QUOTA_EXHAUSTED\",\"message\":\"q\"}"));
        AnaMessagingClient ana = client("secret");
        assertTrue(ana.whatsapp().sendText("91987", "x").data.isQueued());
        assertEquals(2, n.get());
        assertThrows(QuotaExceededException.class, () -> ana.sms().send("91987", "x"));
        assertEquals(1, count("/sms/sent"));
    }

    @Test
    void validationErrors() {
        handlers.put("POST /api/v1/anamsg/send/whatsapp", (c, ex) -> reply(ex, 400, "{\"success\":false,\"message\":\"Request validation failed\",\"errors\":[\"to too short\"]}"));
        ValidationException e = assertThrows(ValidationException.class, () -> client("secret").whatsapp().sendText("1", "x"));
        assertEquals(List.of("to too short"), e.getErrors());
    }

    @Test
    void healthCampaignsAndPreIssuedToken() {
        handlers.put("GET /api/v1/anamsg/health", (c, ex) -> reply(ex, 200, "{\"status\":\"ok\"}"));
        handlers.put("GET /api/v1/anamsg/campaigns/5", (c, ex) -> reply(ex, 200,
                "{\"success\":true,\"data\":{\"campaign\":{\"id\":\"5\",\"results\":{\"sent\":3}},\"recipients\":[],\"skippedRecipients\":[{\"to\":\"91\",\"reason\":\"NO_OPT_IN\"}]}}"));
        AnaMessagingClient ana = client("secret");
        assertEquals("ok", ana.health().get("status"));
        assertNull(calls.get(0).auth);
        ana.campaigns().list(10, null);
        assertEquals("/api/v1/anamsg/campaigns?limit=10", calls.get(calls.size() - 1).path);

        AnaMessagingClient pre = AnaMessagingClient.builder().clientId(null).clientSecret(null).baseUrl(baseUrl).accessToken("tok", "cs").build();
        ApiResponse<CampaignDetail> d = pre.campaigns().get("5");
        assertEquals(3, d.data.campaign.results.get("sent"));
        assertEquals("NO_OPT_IN", d.data.skippedRecipients.get(0).reason);
        assertEquals("Bearer tok", calls.get(calls.size() - 1).auth);
    }
}
