package com.recursive.domain;

import java.time.Instant;

/**
 * Aggregate root of a translation job and its counter state. Counters are
 * derived from the pages table on load, but kept here as write-through
 * fields so dashboard queries stay cheap.
 */
public final class Job {

    private final String id;
    private final String name;
    private final String sourceFilePath;
    private final Language sourceLanguage;
    private final Language targetLanguage;
    private final String modelName;
    private final String configurationJson;

    private JobStatus status;
    private int totalPages;
    private int completedPages;
    private int totalBlocks;
    private int completedBlocks;
    private int validatedBlocks;
    private int failedBlocks;
    private String errorMessage;
    private final Instant createdAt;
    private Instant updatedAt;

    public Job(String id, String name, String sourceFilePath, Language sourceLanguage,
               Language targetLanguage, String modelName, String configurationJson,
               JobStatus status, int totalPages, int completedPages, int totalBlocks,
               int completedBlocks, int validatedBlocks, int failedBlocks,
               String errorMessage, Instant createdAt, Instant updatedAt) {
        this.id = requireId(id);
        this.name = requireText(name, "name");
        this.sourceFilePath = requireText(sourceFilePath, "sourceFilePath");
        this.sourceLanguage = requireLanguage(sourceLanguage);
        this.targetLanguage = requireLanguage(targetLanguage);
        this.modelName = requireText(modelName, "modelName");
        this.configurationJson = configurationJson;
        this.status = status;
        this.totalPages = totalPages;
        this.completedPages = completedPages;
        this.totalBlocks = totalBlocks;
        this.completedBlocks = completedBlocks;
        this.validatedBlocks = validatedBlocks;
        this.failedBlocks = failedBlocks;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void setStatus(JobStatus status) {
        this.status = requireNonNull(status, "status");
        this.updatedAt = Instant.now();
    }

    public void setPageCounters(int total, int completed) {
        this.totalPages = total;
        this.completedPages = Math.min(completed, total);
        this.updatedAt = Instant.now();
    }

    public void setBlockCounters(int total, int completed, int validated, int failed) {
        this.totalBlocks = total;
        this.completedBlocks = Math.min(completed, total);
        this.validatedBlocks = Math.min(validated, total);
        this.failedBlocks = Math.max(failed, 0);
        this.updatedAt = Instant.now();
    }

    public void fail(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.errorMessage = requireErrorMessage(errorMessage);
        this.updatedAt = Instant.now();
    }

    private static String requireId(String id) {
        requireText(id, "id");
        return id;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static Language requireLanguage(Language language) {
        return requireNonNull(language, "language");
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        return value;
    }

    private static String requireErrorMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("errorMessage must not be blank");
        }
        return message;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String sourceFilePath() { return sourceFilePath; }
    public Language sourceLanguage() { return sourceLanguage; }
    public Language targetLanguage() { return targetLanguage; }
    public String modelName() { return modelName; }
    public String configurationJson() { return configurationJson; }
    public JobStatus status() { return status; }
    public int totalPages() { return totalPages; }
    public int completedPages() { return completedPages; }
    public int totalBlocks() { return totalBlocks; }
    public int completedBlocks() { return completedBlocks; }
    public int validatedBlocks() { return validatedBlocks; }
    public int failedBlocks() { return failedBlocks; }
    public String errorMessage() { return errorMessage; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
