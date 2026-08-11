package com.recursive.domain;

import java.util.Optional;

/**
 * Port for OCR of scanned pages. Consumes a rendered page image and returns
 * recognized text segments with coordinates; used only for pages the text
 * extractor could not read.
 */
public interface OcrEngine {

    Optional<OcrResult> recognize(byte[] pageImage, String imageFormat);
}