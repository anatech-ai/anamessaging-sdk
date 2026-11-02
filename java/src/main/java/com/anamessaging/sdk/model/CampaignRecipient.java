package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignRecipient {
    public Object messageId;
    public String to;
    public String status;
    public String error;
    public String sentAt;
    public String deliveredAt;
    public String readAt;
    public String createdAt;
}
