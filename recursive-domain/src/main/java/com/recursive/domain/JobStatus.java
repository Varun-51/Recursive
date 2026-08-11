package com.recursive.domain;

/**
 * Lifecycle of a translation job. Persisted as the job's {@code status}
 * column; transitions are owned by the application layer.
 */
public enum JobStatus {
    CREATED,
    PROCESSING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}