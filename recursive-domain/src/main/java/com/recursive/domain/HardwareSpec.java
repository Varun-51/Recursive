package com.recursive.domain;

/**
 * Host capabilities, in the units the model recommender reasons about
 * (gigabytes). {@code meetsMinimumRequirements} is deliberately a field, not
 * a method: "minimum" is a policy of the application layer, while the
 * measured numbers are facts.
 */
public record HardwareSpec(
        long totalRamGb,
        long availableRamGb,
        int cpuCores,
        String gpuModel,
        long gpuVramMb,
        long freeDiskGb,
        boolean meetsMinimumRequirements) {
}
