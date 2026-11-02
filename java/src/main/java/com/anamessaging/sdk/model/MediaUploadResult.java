package com.anamessaging.sdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaUploadResult {
    /** Send with {@code SendWhatsAppRequest.mediaUpload(to, mediaUploadId)}. */
    public long mediaUploadId;
    public String mediaType;
    public String mimeType;
    public String filename;
    public long size;
}
