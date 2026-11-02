package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignDetail {
    public Campaign campaign;
    public List<CampaignRecipient> recipients = new ArrayList<>();
    public List<CampaignSkip> skippedRecipients = new ArrayList<>();
}
