package com.recursive.application;

import com.recursive.domain.Language;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.TranslationEngine;

import java.util.Optional;

/**
 * Single call site for the on-device translation contract: validates the
 * model name at the boundary, then delegates to whatever engine the
 * infrastructure wired in (Ollama CLI, embedded runtime).
 */
public class StandardModelService {

    private final TranslationEngine translator;

    public StandardModelService(TranslationEngine translator) {
        this.translator = translator;
    }

    public Optional<String> translate(String text, Language source, Language target,
                                      ProcessingContext context, String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("modelName must not be blank");
        }
        return translator.translate(text, source, target, context, modelName);
    }
}
