package com.anamessaging.sdk.exception;

/** Network failure or timeout. */
public class ConnectionException extends AnaMessagingException {
    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
