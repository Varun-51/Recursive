package com.recursive.domain;

/**
 * Bounding box of a block or image, in PDF points, relative to the page
 * origin (bottom-left, as PDF stores it).
 */
public record Position(float x, float y, float width, float height) {

    public Position {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Position dimensions must not be negative");
        }
    }
}