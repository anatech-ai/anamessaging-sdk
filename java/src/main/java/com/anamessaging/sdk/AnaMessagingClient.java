package com.anamessaging.sdk;

import com.anamessaging.sdk.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ANA Messaging API client. Create one and reuse it; it is thread-safe and refreshes its token.
 *
 * <pre>{@code
 * AnaMessagingClient ana = AnaMessagingClient.builder()
 *     .clientId("...").clientSecret("...")
 *     .build();
 * ana.whatsapp().sendText("919876543210", "Hello");
 * ana.sms().send(SendSmsRequest.to("919876543210", "Your OTP is 1234"));
 * }</pre>
 *
 * Credentials are issued on anamessaging.com to organisations with an <b>active API
 * subscription</b>; without one every call throws {@code SubscriptionInactiveException}.
 */
public final class AnaMessagingClient {
    public static final String VERSION = "1.0.0";
    public static final String PRODUCTION_BASE_URL = "https://api.anamessaging.com";
    public static final String SANDBOX_BASE_URL = "https://api-dev.anamessaging.com";

    private final Transport transport;
    private final WhatsApp whatsapp;
    private final Sms sms;
    private final Campaigns campaigns;
    private final Media media;

    private AnaMessagingClient(Config config) {
        this.transport = new Transport(config);
        this.whatsapp = new WhatsApp(transport);
        this.sms = new Sms(transport);
        this.campaigns = new Campaigns(transport);
        this.media = new Media(transport);
    }

    public static Builder builder() { return new Builder(); }

    /**
     * Client configured from {@code ANAMESSAGING_CLIENT_ID}, {@code ANAMESSAGING_CLIENT_SECRET},
     * and optionally {@code ANAMESSAGING_BASE_URL}, {@code ANAMESSAGING_SCOPE}.
     */
    public static AnaMessagingClient fromEnv() { return builder().build(); }

    public WhatsApp whatsapp() { return whatsapp; }
    public Sms sms() { return sms; }
    public Campaigns campaigns() { return campaigns; }
    public Media media() { return media; }

    /** Fetch a token now - validates credentials and subscription at startup. */
    public OAuth2Token authenticate() { return transport.fetchToken(); }

    /** API health; needs no credentials. */
    public Map<String, Object> health() {
        return transport.request("GET", "/health", null, null, false, new TypeReference<Map<String, Object>>() {});
    }

    // ─── Resources ──────────────────────────────────────────────────────────

    /** WhatsApp sends. Calls return once the message is queued, not delivered. */
    public static final class WhatsApp {
        private static final TypeReference<ApiResponse<MessageDispatchResult>> ONE = new TypeReference<>() {};
        private static final TypeReference<ApiResponse<BulkDispatchSummary>> BULK = new TypeReference<>() {};
        private final Transport t;

        WhatsApp(Transport t) { this.t = t; }

        public ApiResponse<MessageDispatchResult> sendText(String to, String text) {
            return send(SendWhatsAppRequest.text(to, text));
        }

        public ApiResponse<MessageDispatchResult> sendTemplate(String to, String templateName, String language, TemplateComponent... components) {
            return send(SendWhatsAppRequest.template(to, templateName).language(language == null ? "en" : language).components(components));
        }

        /** {@code POST /send/whatsapp} with any of the three body shapes. */
        public ApiResponse<MessageDispatchResult> send(SendWhatsAppRequest request) {
            return t.request("POST", "/send/whatsapp", null, t.jsonBody(request), true, ONE);
        }

        /** Template with an image / video / document header: the file goes as multipart. */
        public ApiResponse<MessageDispatchResult> send(SendWhatsAppRequest request, FileAttachment attachment) {
            if (attachment == null) return send(request);
            return t.request("POST", "/send/whatsapp", null, t.multipartBody(request, attachment), true, ONE);
        }

        /** One template to many recipients (not recorded as a campaign). */
        public ApiResponse<BulkDispatchSummary> sendBulk(SendWhatsAppBulkRequest request) { return sendBulk(request, null); }

        public ApiResponse<BulkDispatchSummary> sendBulk(SendWhatsAppBulkRequest request, FileAttachment attachment) {
            Transport.Body body = attachment == null ? t.jsonBody(request) : t.multipartBody(request, attachment);
            return t.request("POST", "/send/whatsapp/bulk", null, body, true, BULK);
        }
    }

    public static final class Sms {
        private static final TypeReference<ApiResponse<SendSmsResult>> TYPE = new TypeReference<>() {};
        private final Transport t;

        Sms(Transport t) { this.t = t; }

        /** Queue an SMS to {@code to} or every number in {@code recipients}. */
        public ApiResponse<SendSmsResult> send(SendSmsRequest request) {
            if ((request.to == null || request.to.isEmpty()) && (request.recipients == null || request.recipients.isEmpty())) {
                throw new IllegalArgumentException("SMS needs `to` or `recipients`");
            }
            return t.request("POST", "/sms/sent", null, t.jsonBody(request), true, TYPE);
        }

        public ApiResponse<SendSmsResult> send(String to, String message) { return send(SendSmsRequest.to(to, message)); }
    }

    public static final class Campaigns {
        private static final TypeReference<ApiResponse<RunCampaignResult>> RUN = new TypeReference<>() {};
        private static final TypeReference<ApiResponse<CampaignList>> LIST = new TypeReference<>() {};
        private static final TypeReference<ApiResponse<CampaignDetail>> GET = new TypeReference<>() {};
        private final Transport t;

        Campaigns(Transport t) { this.t = t; }

        /** Recorded campaign (opt-in and 24h rules applied). Set {@code name} on the request. */
        public ApiResponse<RunCampaignResult> run(SendWhatsAppBulkRequest request) { return run(request, null); }

        public ApiResponse<RunCampaignResult> run(SendWhatsAppBulkRequest request, FileAttachment attachment) {
            Transport.Body body = attachment == null ? t.jsonBody(request) : t.multipartBody(request, attachment);
            return t.request("POST", "/campaigns", null, body, true, RUN);
        }

        public ApiResponse<CampaignList> list(Integer limit, Integer offset) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("limit", limit);
            q.put("offset", offset);
            return t.request("GET", "/campaigns", q, null, true, LIST);
        }

        public ApiResponse<CampaignDetail> get(String campaignId) {
            return t.request("GET", "/campaigns/" + URLEncoder.encode(campaignId, StandardCharsets.UTF_8), null, null, true, GET);
        }
    }

    public static final class Media {
        private static final TypeReference<ApiResponse<MediaUploadResult>> TYPE = new TypeReference<>() {};
        private final Transport t;

        Media(Transport t) { this.t = t; }

        /** Upload once; send with {@code SendWhatsAppRequest.mediaUpload(to, id)}. mediaType may be null. */
        public ApiResponse<MediaUploadResult> upload(FileAttachment file, String mediaType) {
            Map<String, Object> fields = new LinkedHashMap<>();
            if (mediaType != null) fields.put("mediaType", mediaType);
            return t.request("POST", "/media", null, t.multipartBody(fields, file), true, TYPE);
        }
    }

    // ─── Configuration ──────────────────────────────────────────────────────

    static final class Config {
        String clientId, clientSecret, scope, accessToken, csrfToken, baseUrl;
        Duration timeout;
        int maxRetries;
    }

    public static final class Builder {
        private final Config c = new Config();

        private Builder() {
            c.clientId = System.getenv("ANAMESSAGING_CLIENT_ID");
            c.clientSecret = System.getenv("ANAMESSAGING_CLIENT_SECRET");
            c.scope = System.getenv("ANAMESSAGING_SCOPE");
            c.baseUrl = System.getenv("ANAMESSAGING_BASE_URL");
            c.timeout = Duration.ofSeconds(30);
            c.maxRetries = 2;
        }

        /** Client id issued on anamessaging.com. */
        public Builder clientId(String v) { c.clientId = v; return this; }
        /** Client secret issued on anamessaging.com (shown once). */
        public Builder clientSecret(String v) { c.clientSecret = v; return this; }
        /** Optional scope {@code <client id>/<org slug>-<org id>}. */
        public Builder scope(String v) { c.scope = v; return this; }
        /** Use an already-issued token instead of client credentials (not refreshed). */
        public Builder accessToken(String accessToken, String csrfToken) { c.accessToken = accessToken; c.csrfToken = csrfToken; return this; }
        /** API host; default production. */
        public Builder baseUrl(String v) { c.baseUrl = v; return this; }
        public Builder sandbox() { c.baseUrl = SANDBOX_BASE_URL; return this; }
        public Builder timeout(Duration v) { c.timeout = v; return this; }
        /** Retries on 429 throttling and 503, honouring Retry-After. Default 2. */
        public Builder maxRetries(int v) { c.maxRetries = v; return this; }

        public AnaMessagingClient build() {
            if ((c.clientId == null || c.clientSecret == null) && c.accessToken == null) {
                throw new IllegalStateException("ANA Messaging SDK: set clientId and clientSecret (issued on anamessaging.com "
                        + "with an active subscription), or accessToken.");
            }
            String base = c.baseUrl == null || c.baseUrl.isEmpty() ? PRODUCTION_BASE_URL : c.baseUrl;
            c.baseUrl = base.replaceAll("/+$", "");
            return new AnaMessagingClient(c);
        }
    }
}
