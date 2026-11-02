package com.anamessaging.sdk;

import com.anamessaging.sdk.exception.*;
import com.anamessaging.sdk.model.FileAttachment;
import com.anamessaging.sdk.model.OAuth2Token;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Token lifecycle, JSON / multipart encoding, error mapping and retries. Thread-safe. */
final class Transport {
    static final String API_PREFIX = "/api/v1/anamsg";
    private static final long REFRESH_MARGIN_SECONDS = 60;

    final ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final AnaMessagingClient.Config config;
    private final HttpClient http;

    private String accessToken;
    private String csrfToken;
    private Instant expiresAt;

    Transport(AnaMessagingClient.Config config) {
        this.config = config;
        this.http = HttpClient.newBuilder().connectTimeout(config.timeout).build();
        if (config.accessToken != null && !canRefresh()) {
            this.accessToken = config.accessToken;
            this.csrfToken = config.csrfToken;
            this.expiresAt = Instant.MAX;
        }
    }

    private boolean canRefresh() {
        return config.clientId != null && config.clientSecret != null;
    }

    OAuth2Token fetchToken() {
        if (!canRefresh()) throw new AnaMessagingException("clientId and clientSecret are required to obtain a token", 0, null, null, null);
        ObjectNode body = mapper.createObjectNode()
                .put("grant_type", "client_credentials")
                .put("client_id", config.clientId)
                .put("client_secret", config.clientSecret);
        if (config.scope != null) body.put("scope", config.scope);
        return send("POST", "/oauth2", null, jsonBody(body), false, new TypeReference<OAuth2Token>() {});
    }

    private synchronized String[] token() {
        if (accessToken != null && expiresAt.minusSeconds(REFRESH_MARGIN_SECONDS).isAfter(Instant.now())) {
            return new String[] {accessToken, csrfToken};
        }
        OAuth2Token t = fetchToken();
        Instant exp;
        try {
            exp = t.expires_at != null ? Instant.parse(t.expires_at) : Instant.now().plusSeconds(t.expires_in);
        } catch (RuntimeException e) {
            exp = Instant.now().plusSeconds(t.expires_in);
        }
        accessToken = t.access_token;
        csrfToken = t.csrf_token;
        expiresAt = exp;
        return new String[] {accessToken, csrfToken};
    }

    synchronized void invalidateToken() {
        if (expiresAt != Instant.MAX) accessToken = null;
    }

    // ── requests ──

    static final class Body {
        final byte[] bytes;
        final String contentType;

        Body(byte[] bytes, String contentType) {
            this.bytes = bytes;
            this.contentType = contentType;
        }
    }

    Body jsonBody(Object value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper m = mapper.copy()
                    .setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            return new Body(m.writeValueAsBytes(value), "application/json");
        } catch (IOException e) {
            throw new AnaMessagingException("Could not serialise request: " + e.getMessage(), e);
        }
    }

    /** multipart/form-data: scalars as text, arrays/objects as JSON, the file in {@code file}. */
    Body multipartBody(Object fields, FileAttachment file) {
        String boundary = "----anamessaging" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            JsonNode node = fields == null ? mapper.createObjectNode() : mapper.valueToTree(fields);
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getValue() == null || e.getValue().isNull()) continue;
                String value = e.getValue().isContainerNode() ? mapper.writeValueAsString(e.getValue()) : e.getValue().asText();
                out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + e.getKey() + "\"\r\n\r\n" + value + "\r\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                    + file.getFilename().replace("\"", "") + "\"\r\nContent-Type: " + file.getContentType() + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            out.write(file.getData());
            out.write(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new AnaMessagingException("Could not build multipart body: " + e.getMessage(), e);
        }
        return new Body(out.toByteArray(), "multipart/form-data; boundary=" + boundary);
    }

    <T> T request(String method, String path, Map<String, Object> query, Body body, boolean auth, TypeReference<T> type) {
        try {
            return send(method, path, query, body, auth, type);
        } catch (AuthenticationException e) {
            if (!auth || !canRefresh()) throw e;
            invalidateToken(); // revoked or expired server-side: one fresh attempt
            return send(method, path, query, body, auth, type);
        }
    }

    private <T> T send(String method, String path, Map<String, Object> query, Body body, boolean auth, TypeReference<T> type) {
        StringBuilder url = new StringBuilder(config.baseUrl).append(API_PREFIX).append(path);
        if (query != null) {
            String sep = "?";
            for (Map.Entry<String, Object> q : query.entrySet()) {
                if (q.getValue() == null) continue;
                url.append(sep).append(URLEncoder.encode(q.getKey(), StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(String.valueOf(q.getValue()), StandardCharsets.UTF_8));
                sep = "&";
            }
        }
        for (int attempt = 0; ; attempt++) {
            HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url.toString()))
                    .timeout(config.timeout)
                    .header("Accept", "application/json")
                    .header("User-Agent", "anamessaging-sdk-java/" + AnaMessagingClient.VERSION);
            if (auth) {
                String[] t = token();
                rb.header("Authorization", "Bearer " + t[0]);
                if (t[1] != null && !t[1].isEmpty()) rb.header("x-csrf-token", t[1]);
            }
            if (body != null) {
                rb.header("Content-Type", body.contentType);
                rb.method(method, HttpRequest.BodyPublishers.ofByteArray(body.bytes));
            } else {
                rb.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response;
            try {
                response = http.send(rb.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new ConnectionException("Could not reach ANA Messaging at " + config.baseUrl + ": " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectionException("Interrupted while calling ANA Messaging", e);
            }

            int status = response.statusCode();
            String text = response.body();
            if (status >= 200 && status < 300) {
                try {
                    return text == null || text.isEmpty() ? null : mapper.readValue(text, type);
                } catch (IOException e) {
                    throw new AnaMessagingException("Unexpected response from ANA Messaging: " + e.getMessage(), status, null, null, text);
                }
            }
            AnaMessagingException error = toException(status, text, response.headers().firstValue("Retry-After").orElse(null));
            boolean retryable = (error instanceof RateLimitException && !(error instanceof QuotaExceededException))
                    || (error instanceof ServerException && status == 503);
            if (!retryable || attempt >= config.maxRetries) throw error;
            Double wait = error instanceof RateLimitException ? ((RateLimitException) error).getRetryAfterSeconds() : null;
            sleep(wait != null ? (long) (wait * 1000) : 500L << attempt);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted while waiting to retry", e);
        }
    }

    AnaMessagingException toException(int status, String text, String retryAfter) {
        JsonNode node = null;
        try {
            node = text == null || text.isEmpty() ? null : mapper.readTree(text);
        } catch (IOException ignored) {
            // non-JSON body
        }
        String code = str(node, "code");
        if (code == null) code = str(node, "error");
        String message = str(node, "error_description");
        if (message == null) message = str(node, "message");
        if (message == null) message = "HTTP " + status;
        List<String> errors = null;
        if (node != null && node.path("errors").isArray()) {
            errors = new ArrayList<>();
            for (JsonNode e : node.path("errors")) errors.add(e.asText());
        }

        if (status == 400 || status == 413 || status == 422) return new ValidationException(message, status, code, errors, text);
        if (status == 401) return new AuthenticationException(message, status, code, errors, text);
        if (status == 403) {
            String lower = message.toLowerCase();
            if ("NO_SUBSCRIPTION".equals(code) || "SUBSCRIPTION_EXPIRED".equals(code)
                    || lower.contains("subscription") || lower.contains("api access is not enabled")) {
                return new SubscriptionInactiveException(message, status, code, errors, text);
            }
            return new PermissionDeniedException(message, status, code, errors, text);
        }
        if (status == 404) return new NotFoundException(message, status, code, errors, text);
        if (status == 429) {
            Double ra = null;
            try {
                ra = retryAfter != null ? Double.valueOf(retryAfter) : null;
            } catch (NumberFormatException ignored) {
                // HTTP-date form: no hint
            }
            return "QUOTA_EXHAUSTED".equals(code)
                    ? new QuotaExceededException(message, status, code, errors, text, ra)
                    : new RateLimitException(message, status, code, errors, text, ra);
        }
        if (status >= 500) return new ServerException(message, status, code, errors, text);
        return new AnaMessagingException(message, status, code, errors, text);
    }

    private static String str(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field) || !node.get(field).isValueNode()) return null;
        return node.get(field).asText();
    }
}
