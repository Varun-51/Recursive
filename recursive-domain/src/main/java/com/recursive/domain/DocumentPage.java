package com.recursive.domain;

import java.util.List;

/**
 * Raw, un-chunked content of one PDF page. {@code requiresOcr} is set by the
 * parser when the page yields no extractable text (scanned page); the
 * ingestion service then routes the page through the OCR pipeline.
 */
public record DocumentPage(
        int pageNumber,
        List<TextSegment> textSegments,
        List<ImageRegion> imageRegions,
        boolean requiresOcr) {

    public DocumentPage {
        textSegments = List.copyOf(textSegments);
        imageRegions = List.copyOf(imageRegions);
    }
}
