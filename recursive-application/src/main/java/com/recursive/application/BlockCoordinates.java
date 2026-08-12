package com.recursive.application;

/**
 * The three identifiers that pin one block into the document tree: its own
 * id plus the page and job it belongs to.
 */
public record BlockCoordinates(String blockId, String pageId, String jobId) {
}
