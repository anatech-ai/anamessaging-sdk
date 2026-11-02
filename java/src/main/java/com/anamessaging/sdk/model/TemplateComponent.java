package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

/**
 * Header, body or button parameters of a template.
 * {@code TemplateComponent.body("Ravi", "SO-1042")} fills {{1}} and {{2}} of the body.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateComponent {
    /** {@code header}, {@code body} or {@code button}. */
    public String type;
    /** For buttons: {@code url}, {@code quick_reply}. */
    public String sub_type;
    /** For buttons: position as a string ("0", "1"). */
    public String index;
    public List<TemplateParameter> parameters = new ArrayList<>();

    public TemplateComponent() {}

    public TemplateComponent(String type, List<TemplateParameter> parameters) {
        this.type = type;
        this.parameters = parameters;
    }

    public static TemplateComponent body(String... texts) {
        List<TemplateParameter> ps = new ArrayList<>();
        for (String t : texts) ps.add(TemplateParameter.text(t));
        return new TemplateComponent("body", ps);
    }

    public static TemplateComponent headerText(String text) {
        List<TemplateParameter> ps = new ArrayList<>();
        ps.add(TemplateParameter.text(text));
        return new TemplateComponent("header", ps);
    }

    public static TemplateComponent urlButton(int index, String urlSuffix) {
        List<TemplateParameter> ps = new ArrayList<>();
        ps.add(TemplateParameter.text(urlSuffix));
        TemplateComponent c = new TemplateComponent("button", ps);
        c.sub_type = "url";
        c.index = String.valueOf(index);
        return c;
    }
}
