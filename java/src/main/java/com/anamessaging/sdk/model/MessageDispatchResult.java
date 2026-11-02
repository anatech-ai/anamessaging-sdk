package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** One recipient's outcome. {@code status = "queued"} means accepted for delivery, not yet delivered. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageDispatchResult {
    /** Message id on the ANA platform; keep it to reconcile delivery webhooks. */
    public Object messageId;
    /** Normalised destination number. */
    public String to;
    /** {@code queued} or {@code failed}. */
    public String status;
    public String pubSubMessageId;
    /** Why this recipient was not queued. */
    public String error;

    public boolean isQueued() { return "queued".equalsIgnoreCase(status); }

    @Override
    public String toString() { return "MessageDispatchResult{messageId=" + messageId + ", to=" + to + ", status=" + status + (error != null ? ", error=" + error : "") + "}"; }
}
