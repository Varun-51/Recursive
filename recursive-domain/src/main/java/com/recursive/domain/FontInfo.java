package com.recursive.domain;

/**
 * Typographic metadata of a text segment, used to style the translated text
 * during reconstruction.
 */
public record FontInfo(String name, float size, FontStyle style) {

    public FontInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Font name must not be blank");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Font size must be positive");
        }
        if (style == null) {
            throw new IllegalArgumentException("Font style must not be null");
        }
    }
}
