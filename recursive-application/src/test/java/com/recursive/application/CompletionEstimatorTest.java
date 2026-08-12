package com.recursive.application;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompletionEstimatorTest {

    @Test
    void fallsBackToNominalBeforeAnyMeasurement() {
        CompletionEstimator estimator = new CompletionEstimator();

        Duration estimate = estimator.estimateRemaining(30, 3);

        assertEquals(Duration.ofMinutes(30), estimate);
        assertEquals(CompletionEstimator.NOMINAL_MILLIS_PER_PAGE_AT_ONE_SLOT,
                estimator.millisPerPageAtOneSlot());
    }

    @Test
    void usesMeasuredSpeedOncePagesComplete() {
        CompletionEstimator estimator = new CompletionEstimator();
        estimator.recordPage(10, Duration.ofMinutes(5).toMillis());

        assertEquals(30_000L, estimator.millisPerPageAtOneSlot());
        assertEquals(Duration.ofMinutes(5), estimator.estimateRemaining(10, 1));
    }

    @Test
    void moreSlotsShortenTheEstimate() {
        CompletionEstimator estimator = new CompletionEstimator();

        Duration atOne = estimator.estimateRemaining(30, 1);
        Duration atThree = estimator.estimateRemaining(30, 3);

        assertTrue(atThree.compareTo(atOne) < 0);
        assertEquals(Duration.ofMinutes(90), atOne);
        assertEquals(atOne, atThree.multipliedBy(3));
    }

    @Test
    void emptyWorkAndZeroSlotsAreSafe() {
        CompletionEstimator estimator = new CompletionEstimator();

        assertEquals(Duration.ZERO, estimator.estimateRemaining(0, 0));
        assertEquals(Duration.ofMinutes(3), estimator.estimateRemaining(1, 0));
    }

    @Test
    void benchmarkThresholdIsThirtyMinutes() {
        assertTrue(CompletionEstimator.overBenchmark(Duration.ofMinutes(31)));
        assertFalse(CompletionEstimator.overBenchmark(Duration.ofMinutes(30)));
        assertFalse(CompletionEstimator.overBenchmark(Duration.ofMinutes(5)));
    }
}
