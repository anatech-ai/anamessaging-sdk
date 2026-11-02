package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Body of {@code POST /api/v1/anamsg/sms/sent}. Indian A2P traffic needs
 * {@link #principalEntityId} and {@link #dltTemplateId}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendSmsRequest {
    /** One destination number with country code. */
    public String to;
    /** Several numbers; takes precedence over {@link #to}. */
    public List<String> recipients;
    /** Body, 1-1600 chars. */
    public String message;
    /** Approved sender id (max 20). */
    public String senderId;
    /** DLT Principal Entity ID (India). */
    public String principalEntityId;
    /** DLT template id (India). */
    public String dltTemplateId;
    /** Unicode for non-Latin scripts. */
    public Boolean unicode;
    /** Correlation id (max 100). */
    public String campaignId;

    public static SendSmsRequest to(String to, String message) {
        SendSmsRequest r = new SendSmsRequest();
        r.to = to;
        r.message = message;
        return r;
    }

    public static SendSmsRequest toMany(List<String> recipients, String message) {
        SendSmsRequest r = new SendSmsRequest();
        r.recipients = new ArrayList<>(recipients);
        r.message = message;
        return r;
    }

    public static SendSmsRequest toMany(String message, String... recipients) { return toMany(Arrays.asList(recipients), message); }

    public SendSmsRequest senderId(String v) { this.senderId = v; return this; }
    public SendSmsRequest dlt(String principalEntityId, String dltTemplateId) { this.principalEntityId = principalEntityId; this.dltTemplateId = dltTemplateId; return this; }
    public SendSmsRequest unicode(boolean v) { this.unicode = v; return this; }
    public SendSmsRequest campaignId(String v) { this.campaignId = v; return this; }
}
