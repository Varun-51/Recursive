package com.recursive.domain;

/**
 * Per-block verdict of the recursive verification loop. {@code PENDING} is
 * the persisted default; blocks that exhaust the retry budget land in
 * {@code NEEDS_REVIEW} and require a human decision.
 */
public enum ValidationStatus {
    PENDING,
    PASS,
    FAIL,
    NEEDS_REVIEW
}
