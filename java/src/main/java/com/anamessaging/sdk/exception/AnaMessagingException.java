package com.anamessaging.sdk.exception;

import java.util.Collections;
import java.util.List;

/** Base class of every exception the SDK throws (unchecked). */
public class AnaMessagingException extends RuntimeException {
    private final int status;
    private final String code;
    private final List<String> errors;
    private final String responseBody;

    public AnaMessagingException(String message, int status, String code, List<String> errors, String responseBody) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors == null ? Collections.emptyList() : errors;
        this.responseBody = responseBody;
    }

    public AnaMessagingException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
        this.code = null;
        this.errors = Collections.emptyList();
        this.responseBody = null;
    }

    /** HTTP status, or 0 when the API could not be reached. */
    public int getStatus() { return status; }
    /** Machine-readable reason, e.g. {@code QUOTA_EXHAUSTED}, {@code invalid_client}. */
    public String getCode() { return code; }
    /** Field-level validation messages. */
    public List<String> getErrors() { return errors; }
    /** Raw response body. */
    public String getResponseBody() { return responseBody; }
}
