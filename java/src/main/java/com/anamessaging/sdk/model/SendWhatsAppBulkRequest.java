package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Body of {@code POST /send/whatsapp/bulk} (and, with {@link #name}, of {@code POST /campaigns}).
 * Recipients: {@link #recipients}, else {@link #customerIds}, else every active customer.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendWhatsAppBulkRequest {
    public String templateName;
    public String language = "en";
    public List<TemplateComponent> components;
    /** Phone numbers, max 10 000. */
    public List<String> recipients;
    /** ANA customer ids of your organisation, max 10 000. */
    public List<Long> customerIds;
    /** Publish as one event. Not with an attachment. */
    public Boolean useBulkPubSub;
    /** Correlation id (bulk send only). */
    public String campaignId;
    /** Campaign name on the portal (campaigns only). */
    public String name;

    public static SendWhatsAppBulkRequest template(String templateName) {
        SendWhatsAppBulkRequest r = new SendWhatsAppBulkRequest();
        r.templateName = templateName;
        return r;
    }

    public SendWhatsAppBulkRequest language(String language) { this.language = language; return this; }
    public SendWhatsAppBulkRequest components(TemplateComponent... c) { this.components = new ArrayList<>(Arrays.asList(c)); return this; }
    public SendWhatsAppBulkRequest recipients(String... numbers) { this.recipients = new ArrayList<>(Arrays.asList(numbers)); return this; }
    public SendWhatsAppBulkRequest recipients(List<String> numbers) { this.recipients = numbers; return this; }
    public SendWhatsAppBulkRequest customerIds(List<Long> ids) { this.customerIds = ids; return this; }
    public SendWhatsAppBulkRequest useBulkPubSub(boolean v) { this.useBulkPubSub = v; return this; }
    public SendWhatsAppBulkRequest campaignId(String id) { this.campaignId = id; return this; }
    public SendWhatsAppBulkRequest name(String name) { this.name = name; return this; }
}
