package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Body of {@code POST /api/v1/anamsg/send/whatsapp}. Use one of the factories:
 * <ul>
 *   <li>{@link #text(String, String)} - text (inside the 24h window)</li>
 *   <li>{@link #template(String, String)} - approved template, then {@link #language}, {@link #components}</li>
 *   <li>{@link #mediaUrl(String, String, String)} / {@link #mediaUpload(String, long)} - image, video, document, audio</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SendWhatsAppRequest {
    /** Destination with country code, e.g. 919876543210 (7-20 chars). */
    public String to;
    /** Text body, max 4096. */
    public String text;
    /** Approved template name. */
    public String templateName;
    /** Template language; default {@code en}. */
    public String language;
    /** Template parameters. */
    public List<TemplateComponent> components;
    /** image, video, document or audio. */
    public String mediaType;
    /** Id from {@code media().upload(...)}. */
    public Long mediaUploadId;
    /** Public URL of the media. */
    public String mediaUrl;
    /** Caption for an image or video (max 1024). */
    public String caption;
    /** File name shown for a document (max 255). */
    public String filename;
    /** Your correlation id, e.g. an ERP document number (max 100). */
    public String campaignId;

    public static SendWhatsAppRequest text(String to, String text) {
        SendWhatsAppRequest r = new SendWhatsAppRequest();
        r.to = to;
        r.text = text;
        return r;
    }

    public static SendWhatsAppRequest template(String to, String templateName) {
        SendWhatsAppRequest r = new SendWhatsAppRequest();
        r.to = to;
        r.templateName = templateName;
        r.language = "en";
        return r;
    }

    public static SendWhatsAppRequest mediaUrl(String to, String mediaType, String url) {
        SendWhatsAppRequest r = new SendWhatsAppRequest();
        r.to = to;
        r.mediaType = mediaType;
        r.mediaUrl = url;
        return r;
    }

    public static SendWhatsAppRequest mediaUpload(String to, long mediaUploadId) {
        SendWhatsAppRequest r = new SendWhatsAppRequest();
        r.to = to;
        r.mediaUploadId = mediaUploadId;
        return r;
    }

    public SendWhatsAppRequest language(String language) { this.language = language; return this; }
    public SendWhatsAppRequest components(TemplateComponent... components) { this.components = new ArrayList<>(Arrays.asList(components)); return this; }
    public SendWhatsAppRequest components(List<TemplateComponent> components) { this.components = components; return this; }
    public SendWhatsAppRequest caption(String caption) { this.caption = caption; return this; }
    public SendWhatsAppRequest filename(String filename) { this.filename = filename; return this; }
    public SendWhatsAppRequest mediaType(String mediaType) { this.mediaType = mediaType; return this; }
    public SendWhatsAppRequest campaignId(String campaignId) { this.campaignId = campaignId; return this; }
}
