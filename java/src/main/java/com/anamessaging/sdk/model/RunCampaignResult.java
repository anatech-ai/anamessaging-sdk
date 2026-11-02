package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RunCampaignResult {
    /** Read progress with {@code campaigns().get(campaignId)}. */
    public String campaignId;
    public String status;
    public int campaignSize;
    public int queued;
    public int skipped;
    public List<CampaignSkip> skippedRecipients = new ArrayList<>();
    public int failed;
    public List<Map<String, Object>> errors = new ArrayList<>();
    public String category;
}
