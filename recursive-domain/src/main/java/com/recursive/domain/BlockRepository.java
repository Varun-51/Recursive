package com.recursive.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link Block} entities. Read paths serve the worker
 * pool (unprocessed blocks of a page) and the reconstruction service
 * (all blocks of a page in reading order).
 */
public interface BlockRepository {

    Block save(Block block);

    Optional<Block> findById(String id);

    List<Block> findByPageId(String pageId);

    List<Block> findUnprocessedByPageId(String pageId);

    void deleteByPageId(String pageId);
}
