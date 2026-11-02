package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/** One template parameter (WhatsApp Cloud API format). Usually {@code TemplateParameter.text("...")}. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateParameter {
    public String type;
    public String text;
    public String payload;
    public Map<String, Object> image;
    public Map<String, Object> video;
    public Map<String, Object> document;
    public Map<String, Object> currency;
    public Map<String, Object> date_time;

    public TemplateParameter() {}

    public TemplateParameter(String type) { this.type = type; }

    public static TemplateParameter text(String text) {
        TemplateParameter p = new TemplateParameter("text");
        p.text = text;
        return p;
    }

    public static TemplateParameter payload(String payload) {
        TemplateParameter p = new TemplateParameter("payload");
        p.payload = payload;
        return p;
    }
}
