package com.recursive.application;

import com.recursive.domain.HardwareDetector;
import com.recursive.domain.HardwareSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sizes concurrent translation work to the resources that are available
 * <em>right now</em>. The machine may be shared with other processes, so
 * the budget is not a fixed guess: a background thread re-measures the host
 * every poll interval and grows or shrinks the permit supply accordingly.
 * <p>
 * Two independent caps, whichever is tighter:
 * <ul>
 *   <li>CPU: one slot per physical core minus one, so the UI and the OS
 *       always keep a core to themselves;</li>
 *   <li>RAM: one slot per {@link #RAM_PER_SLOT_GB} of currently available
 *       memory, because every concurrent request pins the model weights and
 *       a context window in RAM.</li>
 * </ul>
 * <p>
 * Workers call {@link #acquire()} before starting a request and
 * {@link #release()} when it finishes. The controller is thread-safe.
 */
public final class ThroughputController implements AutoCloseable {

    /** Rough RAM each concurrent request can hold (weights + context). */
    static final long RAM_PER_SLOT_GB = 2;
    private static final long POLL_INTERVAL_MILLIS = 3000;

    private static final Logger log = LoggerFactory.getLogger(ThroughputController.class);

    private final HardwareDetector hardware;
    private final long pollIntervalMillis;
    private final Semaphore permits;
    private final AtomicInteger budget = new AtomicInteger();
    private final AtomicLong active = new AtomicLong();
    private final Thread monitor;
    private volatile boolean closed;

    public ThroughputController(HardwareDetector hardware, Duration pollInterval) {
        this.hardware = hardware;
        this.pollIntervalMillis = pollInterval.toMillis();
        this.permits = new Semaphore(0);
        budget.set(targetBudget(hardware.detect()));
        permits.release(budget.get());
        this.monitor = new Thread(this::monitorLoop, "throughput-monitor");
        this.monitor.setDaemon(true);
        this.monitor.start();
    }

    /** Convenience constructor for composition roots that want the default interval. */
    public static ThroughputController polling(HardwareDetector hardware) {
        return new ThroughputController(hardware, Duration.ofMillis(POLL_INTERVAL_MILLIS));
    }

    /**
     * Blocks until a slot is free and the host still has room for it. If
     * the budget shrank while we held a permit, the slot is handed back and
     * the wait restarts under the tighter limit.
     */
    public void acquire() throws InterruptedException {
        while (true) {
            permits.acquire();
            if (active.incrementAndGet() <= budget.get()) {
                return;
            }
            active.decrementAndGet();
            permits.release();
            Thread.sleep(50);
        }
    }

    /** Hands a slot back. Pairs with {@link #acquire()}. */
    public void release() {
        active.decrementAndGet();
        permits.release();
    }

    /** The current slot budget, for callers that need a number (e.g. OLLAMA_NUM_PARALLEL). */
    public int parallelSlots() {
        return Math.max(1, budget.get());
    }

    @Override
    public void close() {
        closed = true;
        monitor.interrupt();
    }

    private void monitorLoop() {
        while (!closed) {
            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            int target = targetBudget(hardware.detect());
            int previous = budget.getAndSet(target);
            int difference = target - previous;
            if (difference > 0) {
                permits.release(difference);
                log.debug("Throughput budget grew from {} to {} slots", previous, target);
            } else if (difference < 0) {
                log.debug("Throughput budget shrank from {} to {} slots; active workers {}",
                        previous, target, active.get());
            }
        }
    }

    private static int targetBudget(HardwareSpec spec) {
        int coreSlots = Math.max(1, spec.cpuCores() - 1);
        int ramSlots = (int) Math.max(1, spec.availableRamGb() / RAM_PER_SLOT_GB);
        return Math.min(coreSlots, ramSlots);
    }
}
