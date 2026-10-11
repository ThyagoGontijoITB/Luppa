package com.luppa.api.entity;

import java.time.Instant;

public class EventImage {
    private Long id;
    private String fileName;
    private String storageKey;
    private String url;
    private String mimeType;
    private long fileSize;
    private Integer width;
    private Integer height;
    private Instant createdAt;

    public static EventImage fromPath(Integer eventId, String path) {
        EventImage image = new EventImage();
        image.id = eventId == null ? null : eventId.longValue();
        image.fileName = path;
        image.storageKey = path;
        image.url = path;
        image.mimeType = "application/octet-stream";
        image.createdAt = Instant.now();
        return image;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    public Instant getCreatedAt() { return createdAt; }
}
