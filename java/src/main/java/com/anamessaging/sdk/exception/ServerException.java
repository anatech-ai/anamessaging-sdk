package com.anamessaging.sdk.exception;

import java.util.List;

/** 5xx. */
public class ServerException extends AnaMessagingException {
    public ServerException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
