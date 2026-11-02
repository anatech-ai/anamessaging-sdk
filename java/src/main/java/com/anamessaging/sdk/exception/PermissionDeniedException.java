package com.anamessaging.sdk.exception;

import java.util.List;

/** 403: CSRF, IP allowlist or domain check failed. */
public class PermissionDeniedException extends AnaMessagingException {
    public PermissionDeniedException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
