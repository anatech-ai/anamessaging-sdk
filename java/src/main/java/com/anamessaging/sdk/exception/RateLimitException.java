package com.anamessaging.sdk.exception;

import java.util.List;

/** 429: too many requests. {@link #getRetryAfterSeconds()} says when to retry (may be null). */
public class RateLimitException extends AnaMessagingException {
    private final Double retryAfterSeconds;

    public RateLimitException(String message, int status, String code, List<String> errors, String body, Double retryAfterSeconds) {
        super(message, status, code, errors, body);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Double getRetryAfterSeconds() { return retryAfterSeconds; }
}
