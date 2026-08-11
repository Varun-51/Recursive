package com.recursive.domain;

import java.util.List;

/**
 * OCR output for one page image: recognized text segments with their
 * bounding boxes plus the engine's mean confidence so the pipeline can
 * decide whether low-quality text needs a second pass.
 */
public record OcrResult(List<TextSegment> segments, Confidence meanConfidence) {

    public OcrResult {
        segments = List.copyOf(segments);
    }
}