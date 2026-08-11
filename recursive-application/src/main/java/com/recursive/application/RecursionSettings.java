package com.recursive.application;

import com.recursive.domain.Confidence;

/**
 * Policy of the recursive verification loop: how deep recursion goes and
 * when a block is accepted. Owned by the application layer because the
 * numbers are product policy, not domain facts.
 *
 * @param maxDepth            how many translate/validate cycles a block may
 *                            survive before it lands in NEEDS_REVIEW
 * @param minStablePasses     consecutive PASS verdicts required to accept
 *                            a block early
 * @param confidenceThreshold minimum validation confidence for acceptance
 * @param enabled             master switch; when false every block is
 *                            accepted after its first translation
 */
public record RecursionSettings(
        int maxDepth,
        int minStablePasses,
        Confidence confidenceThreshold,
        boolean enabled) {

    public RecursionSettings {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1");
        }
        if (minStablePasses < 1) {
            throw new IllegalArgumentException("minStablePasses must be >= 1");
        }
        if (confidenceThreshold == null) {
            throw new IllegalArgumentException("confidenceThreshold must not be null");
        }
    }

    public static RecursionSettings defaults() {
        return new RecursionSettings(3, 2, Confidence.of(0.85), true);
    }

    public boolean accepts(Confidence validationConfidence, int passes) {
        return !enabled
                || (passes >= minStablePasses
                && validationConfidence.score() >= confidenceThreshold.score());
    }
}
