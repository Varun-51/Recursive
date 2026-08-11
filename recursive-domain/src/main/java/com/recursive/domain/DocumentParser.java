package com.recursive.domain;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Port for reading a PDF into raw text segments and image regions.
 * Implementations stream page by page so multi-hundred-page documents are
 * never resident in memory at once.
 */
public interface DocumentParser {

    /**
     * @param pdfPath  path to a PDF file
     * @param password password for encrypted documents, or {@code null}
     * @return parsed structure, or empty when the document has no text and no
     *         images (e.g. blank pages only)
     * @throws ParseException when the file cannot be read
     */
    Optional<DocumentStructure> parse(Path pdfPath, String password);
}