package com.recursive.application;

import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the lifecycle of a translation job: creation from user input, and
 * the status transitions a job may traverse. Persistence goes through the
 * injected repository; this class carries no mutable state of its own.
 */
public class JobOrchestrator {

    private final JobRepository jobRepository;

    public JobOrchestrator(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public Job createJob(String name, Path sourceFilePath, Language sourceLanguage,
                         Language targetLanguage, String modelName) {
        Path normalized = sourceFilePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) {
            throw new IllegalArgumentException("Source file does not exist: " + normalized);
        }
        Job job = new Job(UUID.randomUUID().toString(), name, normalized.toString(),
                sourceLanguage, targetLanguage, modelName, null, JobStatus.CREATED,
                0, 0, 0, 0, 0, 0, null, Instant.now(), Instant.now());
        return jobRepository.save(job);
    }

    public Optional<Job> findJob(String jobId) {
        return jobRepository.findById(jobId);
    }

    public Job start(String jobId) {
        return transition(jobId, JobStatus.PROCESSING);
    }

    public Job pause(String jobId) {
        return transition(jobId, JobStatus.PAUSED);
    }

    public Job resume(String jobId) {
        return transition(jobId, JobStatus.PROCESSING);
    }

    public Job cancel(String jobId) {
        return transition(jobId, JobStatus.CANCELLED);
    }

    public Job complete(String jobId) {
        return transition(jobId, JobStatus.COMPLETED);
    }

    public Job fail(String jobId, String errorMessage) {
        Job job = require(jobId);
        job.fail(errorMessage);
        return jobRepository.save(job);
    }

    private Job transition(String jobId, JobStatus target) {
        Job job = require(jobId);
        if (job.status() == JobStatus.COMPLETED || job.status() == JobStatus.CANCELLED) {
            throw new IllegalStateException("Job " + jobId + " is already terminal: " + job.status());
        }
        job.setStatus(target);
        return jobRepository.save(job);
    }

    private Job require(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown job: " + jobId));
    }
}
