package com.anamessaging.sdk.exception;

import java.util.List;

/** 429 {@code QUOTA_EXHAUSTED}: the subscription's allowance is used up. Upgrade or top up. */
public class QuotaExceededException extends RateLimitException {
    public QuotaExceededException(String message, int status, String code, List<String> errors, String body, Double retryAfter) {
        super(message, status, code, errors, body, retryAfter);
    }
}
