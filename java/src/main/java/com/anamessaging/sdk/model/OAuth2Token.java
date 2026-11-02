package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** Token issued by {@code POST /api/v1/anamsg/oauth2}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class OAuth2Token {
    public String access_token;
    public String token_type;
    /** Seconds until expiry. */
    public long expires_in;
    /** {@code <client id>/<org slug>-<org id>}. */
    public String scope;
    /** Bound CSRF token, sent as {@code x-csrf-token}. */
    public String csrf_token;
    /** ISO timestamp of expiry. */
    public String expires_at;
}
