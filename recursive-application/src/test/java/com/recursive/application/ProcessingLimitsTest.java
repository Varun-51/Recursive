package com.recursive.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessingLimitsTest {

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new ProcessingLimits(0, Duration.ZERO, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessingLimits(10, Duration.ofSeconds(-1), 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessingLimits(10, null, 2))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ProcessingLimits(10, Duration.ZERO, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void zeroCooldownIsAllowed() {
        assertThat(new ProcessingLimits(10, Duration.ZERO, 1).cooldownBetweenRuns()).isZero();
    }

    @Test
    void defaultsAreSane() {
        ProcessingLimits defaults = ProcessingLimits.defaults();
        assertThat(defaults.maxBlocksPerRun()).isEqualTo(200);
        assertThat(defaults.cooldownBetweenRuns()).isEqualTo(Duration.ofSeconds(2));
        assertThat(defaults.maxConcurrentPages()).isEqualTo(2);
    }
}