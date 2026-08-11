package com.recursive.domain;

/**
 * Outcome of one recursive verification pass over a block.
 */
public record TranslationResult(
        String translatedText,
        Confidence confidenceScore,
        ValidationStatus validationStatus) {

    public TranslationResult {
        if (translatedText == null) {
            throw new IllegalArgumentException("translatedText must not be null");
        }
    }
}