package com.recursive.domain;

/**
 * One extractable text piece with its exact position on the page. The
 * parser's currency; chunking and classification happen in the application.
 */
public record TextSegment(String text, Position position, FontInfo fontInfo) {

    public TextSegment {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text segment must not be blank");
        }
    }
}