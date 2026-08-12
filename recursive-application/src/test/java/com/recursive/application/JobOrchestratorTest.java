package com.recursive.application;

import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobOrchestratorTest {

    @TempDir
    Path tempDir;

    @Test
    void createJobRejectsMissingSourceFile() {
        JobOrchestrator orchestrator = new JobOrchestrator(new InMemoryJobRepository());
        assertThatThrownBy(() -> orchestrator.createJob("n", tempDir.resolve("missing.pdf"),
                Language.of("en", "English"), Language.of("de", "Deutsch"), "llama3.1:8b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createJobPersistsCreatedJob() throws Exception {
        Path source = tempDir.resolve("in.pdf");
        Files.writeString(source, "x");
        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobOrchestrator orchestrator = new JobOrchestrator(repository);

        Job job = orchestrator.createJob("nightly", source, Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b");

        assertThat(job.status()).isEqualTo(JobStatus.CREATED);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void lifecycleTransitionsInOrder() throws Exception {
        Path source = tempDir.resolve("in.pdf");
        Files.writeString(source, "x");
        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobOrchestrator orchestrator = new JobOrchestrator(repository);
        Job job = orchestrator.createJob("nightly", source, Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b");

        assertThat(orchestrator.start(job.id()).status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(orchestrator.pause(job.id()).status()).isEqualTo(JobStatus.PAUSED);
        assertThat(orchestrator.resume(job.id()).status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(orchestrator.complete(job.id()).status()).isEqualTo(JobStatus.COMPLETED);
    }

    @Test
    void terminalJobsRejectFurtherTransitions() throws Exception {
        Path source = tempDir.resolve("in.pdf");
        Files.writeString(source, "x");
        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobOrchestrator orchestrator = new JobOrchestrator(repository);
        Job job = orchestrator.createJob("nightly", source, Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b");
        orchestrator.complete(job.id());

        assertThatThrownBy(() -> orchestrator.pause(job.id())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failRecordsErrorMessage() throws Exception {
        Path source = tempDir.resolve("in.pdf");
        Files.writeString(source, "x");
        InMemoryJobRepository repository = new InMemoryJobRepository();
        JobOrchestrator orchestrator = new JobOrchestrator(repository);
        Job job = orchestrator.createJob("nightly", source, Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b");

        Job failed = orchestrator.fail(job.id(), "ollama offline");

        assertThat(failed.status()).isEqualTo(JobStatus.FAILED);
        assertThat(failed.errorMessage()).isEqualTo("ollama offline");
    }

    private static class InMemoryJobRepository implements JobRepository {
        private final ConcurrentMap<String, Job> jobs = new ConcurrentHashMap<>();

        @Override
        public Job save(Job job) {
            jobs.put(job.id(), job);
            return job;
        }

        @Override
        public Optional<Job> findById(String id) {
            return Optional.ofNullable(jobs.get(id));
        }

        @Override
        public List<Job> findAll() {
            return new ArrayList<>(jobs.values());
        }

        @Override
        public List<Job> findByStatus(JobStatus status) {
            return jobs.values().stream().filter(j -> j.status() == status).toList();
        }

        @Override
        public void delete(String id) {
            jobs.remove(id);
        }
    }
}
