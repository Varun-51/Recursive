package com.recursive.application;

import com.recursive.domain.GlossaryRepository;
import com.recursive.domain.GlossaryTerm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory cache over {@link GlossaryRepository}. Glossary terms are read
 * for every block of a job, so caching them avoids a database round trip
 * per block. Writes pass through and invalidate the affected job.
 */
public class CachingGlossaryService {

    private final GlossaryRepository repository;
    private final Map<String, List<GlossaryTerm>> byJob = new ConcurrentHashMap<>();
    private final Map<String, List<GlossaryTerm>> lockedByJob = new ConcurrentHashMap<>();

    public CachingGlossaryService(GlossaryRepository repository) {
        this.repository = repository;
    }

    public List<GlossaryTerm> termsFor(String jobId) {
        return byJob.computeIfAbsent(jobId, repository::findByJobId);
    }

    public List<GlossaryTerm> lockedTermsFor(String jobId) {
        return lockedByJob.computeIfAbsent(jobId, repository::findLockedByJobId);
    }

    public GlossaryTerm save(GlossaryTerm term) {
        GlossaryTerm saved = repository.save(term);
        byJob.remove(saved.jobId());
        lockedByJob.remove(saved.jobId());
        return saved;
    }

    public void deleteByJobId(String jobId) {
        repository.deleteByJobId(jobId);
        byJob.remove(jobId);
        lockedByJob.remove(jobId);
    }
}
