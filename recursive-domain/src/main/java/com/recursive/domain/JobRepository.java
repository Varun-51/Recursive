package com.recursive.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link Job} aggregates. Implementations must be
 * thread-safe (multiple workers and the orchestrator share one repository).
 */
public interface JobRepository {

    Job save(Job job);

    Optional<Job> findById(String id);

    List<Job> findAll();

    List<Job> findByStatus(JobStatus status);

    void delete(String id);
}
