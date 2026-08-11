package com.recursive.domain;

import java.nio.file.Path;

/**
 * Port for building the translated PDF from persisted page/block data.
 * Implementations own the layout-engineering details (text expansion,
 * overflow pages, image placement); callers own page ordering.
 */
public interface DocumentReconstructor {

    Path reconstruct(Path outputDirectory, String jobId);
}