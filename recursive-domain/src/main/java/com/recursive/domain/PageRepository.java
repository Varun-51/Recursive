package com.recursive.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link Page} entities. {@code findIncomplete} backs
 * crash recovery and resume: it returns every page of a job that did not
 * finish.
 */
public interface PageRepository {

    Page save(Page page);

    Optional<Page> findById(String id);

    List<Page> findByJobId(String jobId);

    List<Page> findIncompleteByJobId(String jobId);

    void deleteByJobId(String jobId);
}