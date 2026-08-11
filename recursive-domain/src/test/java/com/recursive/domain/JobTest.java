package com.recursive.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobTest {

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(() -> newJob(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> newJob("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void failTransitionsToFailedWithMessage() {
        Job job = newJob("j1");
        job.fail("model unavailable");
        assertThat(job.status()).isEqualTo(JobStatus.FAILED);
        assertThat(job.errorMessage()).isEqualTo("model unavailable");
    }

    @Test
    void failRejectsBlankMessage() {
        Job job = newJob("j1");
        assertThatThrownBy(() -> job.fail(" ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void countersNeverGoNegativeOrPastTotal() {
        Job job = newJob("j1");
        job.setBlockCounters(10, 5, 7, -1);
        assertThat(job.totalBlocks()).isEqualTo(10);
        assertThat(job.completedBlocks()).isEqualTo(5);
        assertThat(job.failedBlocks()).isZero();
    }

    private static Job newJob(String id) {
        return new Job(id, "nightly", "C:/docs/source.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", null,
                JobStatus.CREATED, 0, 0, 0, 0, 0, 0, null, Instant.now(), Instant.now());
    }
}