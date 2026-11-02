package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A recipient a campaign skipped: NO_OPT_IN, SENT_IN_LAST_24H, DUPLICATE or QUEUE_ERROR. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CampaignSkip {
    public String to;
    public String reason;
    public String detail;
}
