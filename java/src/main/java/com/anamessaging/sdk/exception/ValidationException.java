package com.anamessaging.sdk.exception;

import java.util.List;

/** 400 / 413: the request was rejected; fix the payload. */
public class ValidationException extends AnaMessagingException {
    public ValidationException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
