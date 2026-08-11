package com.recursive.domain;

/**
 * A raster embedded in the page. Images are preserved, never translated;
 * carrying the bytes here lets ingestion persist them to the {@code images}
 * table without re-reading the PDF.
 */
public record ImageRegion(
        int imageIndex,
        Position position,
        byte[] imageData,
        String originalFormat) {

    public ImageRegion {
        if (position == null) {
            throw new IllegalArgumentException("position must not be null");
        }
        if (imageData == null) {
            throw new IllegalArgumentException("imageData must not be null; pass an empty array when absent");
        }
    }
}