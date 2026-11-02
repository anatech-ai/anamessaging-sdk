package com.anamessaging.sdk.exception;

import java.util.List;

/** 403: no active ANA Messaging subscription (missing, expired or API access disabled). Renew on anamessaging.com. */
public class SubscriptionInactiveException extends PermissionDeniedException {
    public SubscriptionInactiveException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
