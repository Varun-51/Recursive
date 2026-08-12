package com.recursive.domain;

import java.util.List;

/**
 * Full parse result of a PDF. Pages are streamed out of the parser one at a
 * time in practice, but the type itself is a simple value object.
 */
public record DocumentStructure(String fileName, List<DocumentPage> pages) {

    public DocumentStructure {
        pages = List.copyOf(pages);
    }
}
