package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Envelope of every API response (other than the token call). */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse<T> {
    /** True when the call was accepted. */
    public boolean success;
    /** Human-readable summary, e.g. "WhatsApp message queued for delivery". */
    public String message;
    /** Typed payload of a successful call. */
    public T data;
    /** Validation errors on failure. */
    public List<String> errors;
    /** Machine-readable failure reason. */
    public String code;
    /** ISO-8601 server timestamp. */
    public String timestamp;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
