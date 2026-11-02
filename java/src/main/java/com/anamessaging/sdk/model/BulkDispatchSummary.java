package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkDispatchSummary {
    public int campaignSize;
    public int queued;
    public int failed;
    public List<MessageDispatchResult> messages = new ArrayList<>();
    /** {@code {to, error}} per failed recipient. */
    public List<Map<String, Object>> errors = new ArrayList<>();
    public String pubSubMessageId;
}
