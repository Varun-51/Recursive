package com.recursive.application;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Estimates how long the remaining translation work will take, based on the
 * resources available right now. Before any page has finished it falls back
 * to a nominal baseline ({@link #NOMINAL_MILLIS_PER_PAGE_AT_ONE_SLOT}); once
 * pages complete, the wall-clock average per page is used instead, so the
 * ETA tracks the machine's real speed. Thread-safe: progress callbacks run
 * outside the JavaFX thread, the estimation runs on it.
 */
public final class CompletionEstimator {

    /**
     * Baseline: one page at one parallel slot. Tuned for a 7B model on
     * CPU (a page is roughly 5-15 blocks, each needing two LLM passes).
     */
    static final long NOMINAL_MILLIS_PER_PAGE_AT_ONE_SLOT = 3 * 60 * 1000L;

    /** Minutes above which the user is asked whether to wait for more resources. */
    public static final long BENCHMARK_MINUTES = 30;

    private final AtomicLong measuredMillisPerPage = new AtomicLong(-1);
    private final AtomicLong lastRecorded = new AtomicLong();

    /**
     * Records that one page finished; {@code wallMillis} is the run time so
     * far. Out-of-order callbacks are ignored so the average never steps
     * backwards.
     */
    public void recordPage(long completedPages, long wallMillis) {
        long previous = lastRecorded.get();
        if (completedPages > previous && lastRecorded.compareAndSet(previous, completedPages)) {
            measuredMillisPerPage.set(wallMillis / completedPages);
        }
    }

    public void reset() {
        lastRecorded.set(0);
        measuredMillisPerPage.set(-1);
    }

    /** Milliseconds one page takes at one parallel slot, measured or nominal. */
    public long millisPerPageAtOneSlot() {
        long measured = measuredMillisPerPage.get();
        return measured > 0 ? measured : NOMINAL_MILLIS_PER_PAGE_AT_ONE_SLOT;
    }

    /**
     * Remaining time with {@code pendingPages} left and {@code parallelSlots}
     * workers running concurrently.
     */
    public Duration estimateRemaining(int pendingPages, int parallelSlots) {
        int slots = Math.max(1, parallelSlots);
        long millis = millisPerPageAtOneSlot() * Math.max(0, pendingPages) / slots;
        return Duration.ofMillis(millis);
    }

    public static boolean overBenchmark(Duration estimate) {
        return estimate.toMinutes() > BENCHMARK_MINUTES;
    }
}
