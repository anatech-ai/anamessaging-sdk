package com.anamessaging.sdk.model;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;

/** A file sent as multipart/form-data (template media header, or media upload). */
public class FileAttachment {
    private final byte[] data;
    private final String filename;
    private final String contentType;

    public FileAttachment(byte[] data, String filename, String contentType) {
        this.data = data;
        this.filename = filename;
        this.contentType = contentType != null ? contentType : guess(filename);
    }

    public FileAttachment(byte[] data, String filename) { this(data, filename, null); }

    public static FileAttachment fromPath(Path path) throws IOException {
        return new FileAttachment(Files.readAllBytes(path), path.getFileName().toString());
    }

    public byte[] getData() { return data; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }

    private static String guess(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        String t = URLConnection.guessContentTypeFromName(filename);
        return t != null ? t : "application/octet-stream";
    }
}
