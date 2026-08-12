package com.recursive.domain;

import java.util.List;

/**
 * Verdict of the semantic validator on one original/translation pair.
 * Issues are human- and LLM-readable failure descriptions used both for
 * the re-translation prompt and for the review screen.
 */
public record ValidationReport(
        ValidationStatus status,
        List<String> issues,
        Confidence confidenceScore) {

    public ValidationReport {
        issues = List.copyOf(issues);
    }

    public static ValidationReport pass(Confidence confidenceScore) {
        return new ValidationReport(ValidationStatus.PASS, List.of(), confidenceScore);
    }
}
