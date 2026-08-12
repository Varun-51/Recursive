package com.recursive.domain;

import java.time.Instant;

/**
 * A glossary entry enforcing terminological consistency: the translator
 * receives these terms in every block's {@link ProcessingContext} and must
 * reproduce {@code targetTerm} for {@code sourceTerm}. Locked entries are
 * binding; unlocked ones are suggestions.
 */
public final class GlossaryTerm {

    private final String id;
    private final String jobId;
    private final String sourceTerm;
    private final String targetTerm;
    private final String category;
    private final boolean locked;
    private int occurrences;
    private final Instant createdAt;

    public GlossaryTerm(String id, String jobId, String sourceTerm, String targetTerm,
                        String category, boolean locked, int occurrences, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException("jobId must not be blank");
        }
        if (sourceTerm == null || sourceTerm.isBlank()) {
            throw new IllegalArgumentException("sourceTerm must not be blank");
        }
        if (targetTerm == null || targetTerm.isBlank()) {
            throw new IllegalArgumentException("targetTerm must not be blank");
        }
        this.id = id;
        this.jobId = jobId;
        this.sourceTerm = sourceTerm;
        this.targetTerm = targetTerm;
        this.category = category;
        this.locked = locked;
        this.occurrences = occurrences;
        this.createdAt = createdAt;
    }

    public void incrementOccurrences() {
        this.occurrences++;
    }

    public String id() { return id; }
    public String jobId() { return jobId; }
    public String sourceTerm() { return sourceTerm; }
    public String targetTerm() { return targetTerm; }
    public String category() { return category; }
    public boolean locked() { return locked; }
    public int occurrences() { return occurrences; }
    public Instant createdAt() { return createdAt; }
}
