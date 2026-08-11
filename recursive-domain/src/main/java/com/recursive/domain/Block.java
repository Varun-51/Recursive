package com.recursive.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Smallest translatable unit of a page. Layout metadata (position, font)
 * lives on the block so the reconstructor can place translated text without
 * re-parsing the source document.
 */
public final class Block {

    private final String id;
    private final String pageId;
    private final int blockIndex;
    private final BlockContentType contentType;
    private final String originalText;
    private final Position position;
    private final FontInfo fontInfo;
    private final int readingOrder;

    private String translatedText;
    private ValidationStatus validationStatus;
    private Double confidenceScore;
    private int retryCount;
    private String contextJson;
    private String validationIssuesJson;
    private final Instant createdAt;
    private Instant updatedAt;

    public Block(String id, String pageId, int blockIndex, BlockContentType contentType,
                 String originalText, Position position, FontInfo fontInfo, int readingOrder,
                 String translatedText, ValidationStatus validationStatus, Double confidenceScore,
                 int retryCount, String contextJson, String validationIssuesJson,
                 Instant createdAt, Instant updatedAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (pageId == null || pageId.isBlank()) {
            throw new IllegalArgumentException("pageId must not be blank");
        }
        this.id = id;
        this.pageId = pageId;
        this.blockIndex = blockIndex;
        this.contentType = Objects.requireNonNull(contentType, "contentType");
        this.originalText = Objects.requireNonNull(originalText, "originalText");
        this.position = Objects.requireNonNull(position, "position");
        this.fontInfo = fontInfo;
        this.readingOrder = readingOrder;
        this.translatedText = translatedText;
        this.validationStatus = validationStatus;
        this.confidenceScore = confidenceScore;
        this.retryCount = retryCount;
        this.contextJson = contextJson;
        this.validationIssuesJson = validationIssuesJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setTranslatedText(String translatedText) {
        this.translatedText = translatedText;
        this.updatedAt = Instant.now();
    }

    public void setValidationStatus(ValidationStatus validationStatus) {
        this.validationStatus = validationStatus;
        this.updatedAt = Instant.now();
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
        this.updatedAt = Instant.now();
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
        this.updatedAt = Instant.now();
    }

    public void setValidationIssuesJson(String validationIssuesJson) {
        this.validationIssuesJson = validationIssuesJson;
        this.updatedAt = Instant.now();
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
        this.updatedAt = Instant.now();
    }

    public String id() { return id; }
    public String pageId() { return pageId; }
    public int blockIndex() { return blockIndex; }
    public BlockContentType contentType() { return contentType; }
    public String originalText() { return originalText; }
    public Position position() { return position; }
    public FontInfo fontInfo() { return fontInfo; }
    public int readingOrder() { return readingOrder; }
    public String translatedText() { return translatedText; }
    public ValidationStatus validationStatus() { return validationStatus; }
    public Double confidenceScore() { return confidenceScore; }
    public int retryCount() { return retryCount; }
    public String contextJson() { return contextJson; }
    public String validationIssuesJson() { return validationIssuesJson; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}