package com.anamessaging.sdk.exception;

import java.util.List;

/** 401: wrong client id / secret, or the token is invalid, expired or revoked. */
public class AuthenticationException extends AnaMessagingException {
    public AuthenticationException(String message, int status, String code, List<String> errors, String body) {
        super(message, status, code, errors, body);
    }
}
