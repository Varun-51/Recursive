package com.recursive.application;

import com.recursive.domain.HardwareSpec;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThroughputControllerTest {

    private static HardwareSpec spec(long availableRamGb, int cores) {
        return new HardwareSpec(16, availableRamGb, cores, null, 0, 100, true);
    }

    private static ThroughputController controller(AtomicReference<HardwareSpec> spec) {
        return new ThroughputController(spec::get, Duration.ofMillis(20));
    }

    @Test
    void budgetIsTheTighterOfCpuAndRamLimits() throws Exception {
        AtomicReference<HardwareSpec> spec = new AtomicReference<>(spec(6, 8));
        try (ThroughputController controller = controller(spec)) {
            Thread.sleep(50);
            int coreSlots = 7;
            int ramSlots = 3;
            assertEquals(Math.min(coreSlots, ramSlots), controller.parallelSlots());
        }
    }

    @Test
    void budgetGrowsWhenRamFreesUp() throws Exception {
        AtomicReference<HardwareSpec> spec = new AtomicReference<>(spec(2, 8));
        try (ThroughputController controller = controller(spec)) {
            assertEquals(1, controller.parallelSlots());
            spec.set(spec(16, 8));
            Thread.sleep(100);
            assertEquals(7, controller.parallelSlots());
        }
    }

    @Test
    void acquireBlocksWhileTheBudgetIsExhausted() throws Exception {
        AtomicReference<HardwareSpec> spec = new AtomicReference<>(spec(2, 2));
        try (ThroughputController controller = controller(spec)) {
            assertTrue(controller.parallelSlots() >= 1);
            controller.acquire();
            Thread blocked = new Thread(() -> {
                try {
                    controller.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            blocked.start();
            Thread.sleep(100);
            assertEquals(Thread.State.WAITING, blocked.getState());

            controller.release();
            blocked.join(2000);
            assertEquals(Thread.State.TERMINATED, blocked.getState());
        }
    }
}
