package com.recursive.domain;

/**
 * Human-readable language pair. Codes follow the ISO 639-1 style used by
 * Ollama prompts ({@code en}, {@code fr}, {@code de}, ...).
 *
 * @param code short language code
 * @param name display name
 */
public record Language(String code, String name) {

    public Language {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Language code must not be blank");
        }
    }

    public static Language of(String code, String name) {
        return new Language(code, name);
    }
}
