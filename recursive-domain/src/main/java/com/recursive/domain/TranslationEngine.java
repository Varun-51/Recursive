package com.recursive.domain;

import java.util.Optional;

/**
 * Port for on-device translation of a single block. Implementations are
 * free to stream tokens, but the contract returns the final text.
 */
public interface TranslationEngine {

    Optional<String> translate(String text, Language source, Language target,
                               ProcessingContext context, String modelName);
}
