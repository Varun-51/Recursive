package com.recursive.application;

import com.recursive.domain.GlossaryRepository;
import com.recursive.domain.GlossaryTerm;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CachingGlossaryServiceTest {

    @Test
    void cachesRepositoryReadsPerJob() {
        CountingRepository repository = new CountingRepository();
        CachingGlossaryService service = new CachingGlossaryService(repository);

        service.termsFor("j1");
        service.termsFor("j1");

        assertThat(repository.findByJobIdCalls).isEqualTo(1);
    }

    @Test
    void lockedTermsComeFromRepositoryOnly() {
        CountingRepository repository = new CountingRepository();
        CachingGlossaryService service = new CachingGlossaryService(repository);

        assertThat(service.lockedTermsFor("j1")).isEmpty();
        assertThat(repository.findLockedByJobIdCalls).isEqualTo(1);
    }

    @Test
    void saveInvalidatesCachedLists() {
        CountingRepository repository = new CountingRepository();
        CachingGlossaryService service = new CachingGlossaryService(repository);

        service.termsFor("j1");
        service.save(term("j1"));
        service.termsFor("j1");

        assertThat(repository.findByJobIdCalls).isEqualTo(2);
    }

    private static GlossaryTerm term(String jobId) {
        return new GlossaryTerm("t1", jobId, "invoice", "Rechnung", "finance", true, 1, Instant.now());
    }

    private static class CountingRepository implements GlossaryRepository {
        int findByJobIdCalls;
        int findLockedByJobIdCalls;

        @Override
        public GlossaryTerm save(GlossaryTerm term) {
            return term;
        }

        @Override
        public Optional<GlossaryTerm> findById(String id) {
            return Optional.empty();
        }

        @Override
        public List<GlossaryTerm> findByJobId(String jobId) {
            findByJobIdCalls++;
            return new ArrayList<>();
        }

        @Override
        public List<GlossaryTerm> findLockedByJobId(String jobId) {
            findLockedByJobIdCalls++;
            return new ArrayList<>();
        }

        @Override
        public void deleteByJobId(String jobId) {
        }
    }
}