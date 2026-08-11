package com.recursive.domain;

import java.time.Instant;

/**
 * One page of a job. The JSON columns persist the intermediate artifacts
 * (extracted blocks, translated blocks, validation summary) as opaque blobs
 * keyed by enum values; their shape is owned by the application layer so
 * the schema stays stable across prompt changes.
 */
public final class Page {

    private final String id;
    private final String jobId;
    private final int pageNumber;

    private PageStatus status;
    private String extractedJson;
    private String translatedJson;
    private String validationJson;
    private int retryCount;
    private String errorMessage;
    private final Instant createdAt;
    private Instant updatedAt;

    public Page(String id, String jobId, int pageNumber, PageStatus status,
                String extractedJson, String translatedJson, String validationJson,
                int retryCount, String errorMessage, Instant createdAt, Instant updatedAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be >= 1");
        }
        this.id = id;
        this.jobId = jobId;
        this.pageNumber = pageNumber;
        this.status = status;
        this.extractedJson = extractedJson;
        this.translatedJson = translatedJson;
        this.validationJson = validationJson;
        this.retryCount = retryCount;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setStatus(PageStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void setExtractedJson(String extractedJson) {
        this.extractedJson = extractedJson;
        this.updatedAt = Instant.now();
    }

    public void setTranslatedJson(String translatedJson) {
        this.translatedJson = translatedJson;
        this.updatedAt = Instant.now();
    }

    public void setValidationJson(String validationJson) {
        this.validationJson = validationJson;
        this.updatedAt = Instant.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = Instant.now();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = Instant.now();
    }

    public String id() { return id; }
    public String jobId() { return jobId; }
    public int pageNumber() { return pageNumber; }
    public PageStatus status() { return status; }
    public String extractedJson() { return extractedJson; }
    public String translatedJson() { return translatedJson; }
    public String validationJson() { return validationJson; }
    public int retryCount() { return retryCount; }
    public String errorMessage() { return errorMessage; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}