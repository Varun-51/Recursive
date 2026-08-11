package com.recursive.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link GlossaryTerm} entries. Locked terms are fed to
 * every translation call of a job.
 */
public interface GlossaryRepository {

    GlossaryTerm save(GlossaryTerm term);

    Optional<GlossaryTerm> findById(String id);

    List<GlossaryTerm> findByJobId(String jobId);

    List<GlossaryTerm> findLockedByJobId(String jobId);

    void deleteByJobId(String jobId);
}