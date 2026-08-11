package com.recursive.infrastructure.database;

import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcJobRepositoryTest {

    private final Path databaseFile = Path.of("target", "test-db",
            "jobs-" + UUID.randomUUID() + ".db");
    private DatabaseInitializer initializer;

    @AfterEach
    void closeDatabase() {
        if (initializer != null) {
            initializer.close();
        }
    }

    @Test
    void roundTripsJobWithAllFields() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository repository = new JdbcJobRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        Job job = new Job("j1", "nightly", "C:/docs/in.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", "{}", JobStatus.PROCESSING,
                10, 4, 100, 40, 30, 5, null, now, now);

        repository.save(job);
        Job loaded = repository.findById("j1").orElseThrow();

        assertThat(loaded.name()).isEqualTo("nightly");
        assertThat(loaded.sourceLanguage().code()).isEqualTo("en");
        assertThat(loaded.targetLanguage().name()).isEqualTo("Deutsch");
        assertThat(loaded.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(loaded.completedPages()).isEqualTo(4);
        assertThat(loaded.completedBlocks()).isEqualTo(40);
        assertThat(loaded.failedBlocks()).isEqualTo(5);
    }

    @Test
    void upsertUpdatesExistingRow() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository repository = new JdbcJobRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        Language english = Language.of("en", "English");
        Language german = Language.of("de", "Deutsch");
        repository.save(new Job("j1", "first", "C:/docs/in.pdf", english, german, "m1",
                null, JobStatus.CREATED, 0, 0, 0, 0, 0, 0, null, now, now));

        repository.save(new Job("j1", "first", "C:/docs/in.pdf", english, german, "m1",
                null, JobStatus.COMPLETED, 1, 1, 5, 5, 5, 0, null, now, now));

        assertThat(repository.findById("j1").orElseThrow().status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void queriesByStatusAndDeletes() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository repository = new JdbcJobRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        Language english = Language.of("en", "English");
        Language german = Language.of("de", "Deutsch");
        repository.save(new Job("j1", "a", "C:/docs/in.pdf", english, german, "m1",
                null, JobStatus.COMPLETED, 0, 0, 0, 0, 0, 0, null, now, now));
        repository.save(new Job("j2", "b", "C:/docs/in.pdf", english, german, "m1",
                null, JobStatus.FAILED, 0, 0, 0, 0, 0, 0, "boom", now, now));

        List<Job> failed = repository.findByStatus(JobStatus.FAILED);
        assertThat(failed).extracting(Job::id).containsExactly("j2");
        assertThat(failed.get(0).errorMessage()).isEqualTo("boom");

        repository.delete("j2");
        assertThat(repository.findAll()).extracting(Job::id).containsExactly("j1");
    }

    @Test
    void schemaInitializationIsIdempotent() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        initializer.initialize();
        assertThat(databaseFile).exists();
    }
}
