package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Campaign {
    public String id;
    public String name;
    public String templateName;
    public String templateLanguage;
    public String category;
    public String status;
    public int totalRecipients;
    public int queued;
    public int skipped;
    /** Counts by status: pending, sent, delivered, read, failed. */
    public Map<String, Integer> results;
    public String createdAt;
}
