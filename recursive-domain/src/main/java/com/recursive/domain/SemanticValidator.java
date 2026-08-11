package com.recursive.domain;

import java.util.Optional;

/**
 * Port for the semantic verification of one block. The implementation must
 * check meaning, intent, negation, numbers, units, proper nouns, references,
 * relationships, and terminology — and must surface failures as structured
 * issues usable as re-translation guidance.
 */
public interface SemanticValidator {

    Optional<ValidationReport> validate(String originalText, String translatedText,
                                        ProcessingContext context);
}