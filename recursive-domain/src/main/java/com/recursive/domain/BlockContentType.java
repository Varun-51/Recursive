package com.recursive.domain;

/**
 * Statistical role of a text block inside a page, used by the reconstructor
 * for layout decisions (headers/footers repeat; captions stay near images).
 */
public enum BlockContentType {
    PARAGRAPH,
    HEADING,
    SUBHEADING,
    TABLE_CELL,
    HEADER,
    FOOTER,
    CAPTION,
    LIST_ITEM
}