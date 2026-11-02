package com.anamessaging.sdk.exception;

import java.util.List;

/** 404: e.g. unknown template or campaign. */
public class NotFoundException extends AnaMessagingException {
    public NotFoundException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
