package com.recursive.application;

import java.time.Duration;

/**
 * Throughput limits of a translation run: how much work is released before
 * the pipeline breathes, and how many pages may translate concurrently.
 * Kept as plain policy so operators can tune without touching code.
 *
 * @param maxBlocksPerRun        blocks translated before a cooling pause
 * @param cooldownBetweenRuns    pause between runs, lets the GPU/CPU cool
 * @param maxConcurrentPages     pages translated in parallel
 */
public record ProcessingLimits(
        int maxBlocksPerRun,
        Duration cooldownBetweenRuns,
        int maxConcurrentPages) {

    public ProcessingLimits {
        if (maxBlocksPerRun < 1) {
            throw new IllegalArgumentException("maxBlocksPerRun must be >= 1");
        }
        if (cooldownBetweenRuns == null || cooldownBetweenRuns.isNegative()) {
            throw new IllegalArgumentException("cooldownBetweenRuns must be non-negative");
        }
        if (maxConcurrentPages < 1) {
            throw new IllegalArgumentException("maxConcurrentPages must be >= 1");
        }
    }

    public static ProcessingLimits defaults() {
        return new ProcessingLimits(200, Duration.ofSeconds(2), 2);
    }
}
