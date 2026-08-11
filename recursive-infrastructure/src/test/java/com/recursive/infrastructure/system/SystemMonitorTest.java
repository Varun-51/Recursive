package com.recursive.infrastructure.system;

import com.recursive.domain.HardwareSpec;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemMonitorTest {

    private final SystemMonitor monitor = new SystemMonitor();

    @Test
    void reportsPlausibleFacts() {
        HardwareSpec spec = monitor.detect();

        assertThat(spec.totalRamGb()).isPositive();
        assertThat(spec.availableRamGb()).isBetween(0L, spec.totalRamGb());
        assertThat(spec.cpuCores()).isPositive();
        assertThat(spec.freeDiskGb()).isNotNegative();
    }
}
