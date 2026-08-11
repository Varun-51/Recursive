package com.recursive.domain;

import java.time.Instant;

/**
 * A raster embedded in a translated page; content and coordinates are
 * preserved verbatim (images are never translated).
 */
public final class ImageReference {

    private final String id;
    private final String pageId;
    private final int imageIndex;
    private final Position position;
    private final byte[] imageData;
    private final String originalFormat;
    private final Instant createdAt;

    public ImageReference(String id, String pageId, int imageIndex, Position position,
                          byte[] imageData, String originalFormat, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        this.id = id;
        this.pageId = pageId;
        this.imageIndex = imageIndex;
        this.position = position;
        this.imageData = imageData;
        this.originalFormat = originalFormat;
        this.createdAt = createdAt;
    }

    public String id() { return id; }
    public String pageId() { return pageId; }
    public int imageIndex() { return imageIndex; }
    public Position position() { return position; }
    public byte[] imageData() { return imageData; }
    public String originalFormat() { return originalFormat; }
    public Instant createdAt() { return createdAt; }
}