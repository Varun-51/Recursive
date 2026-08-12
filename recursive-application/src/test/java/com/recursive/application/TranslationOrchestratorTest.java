package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.Confidence;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.GlossaryRepository;
import com.recursive.domain.GlossaryTerm;
import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.PageStatus;
import com.recursive.domain.Position;
import com.recursive.domain.SemanticValidator;
import com.recursive.domain.TranslationEngine;
import com.recursive.domain.ValidationReport;
import com.recursive.domain.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationOrchestratorTest {

    private static final Language EN = Language.of("en", "English");
    private static final Language DE = Language.of("de", "Deutsch");

    @Test
    void acceptsBlockWhenValidationPasses() {
        OrchestratorHarness harness = new OrchestratorHarness()
                .withRecursion(new RecursionSettings(3, 1, Confidence.of(0.85), true))
                .withTranslator((text, source, target, context, model) -> Optional.of("Hallo"))
                .withValidator((original, translated, context) ->
                        Optional.of(ValidationReport.pass(Confidence.of(0.95))));
        harness.translate();

        Block block = harness.onlyBlock();
        assertThat(block.translatedText()).isEqualTo("Hallo");
        assertThat(block.validationStatus()).isEqualTo(ValidationStatus.PASS);
        assertThat(block.retryCount()).isZero();
    }

    @Test
    void retriesUntilStablePassesAreReached() {
        int[] calls = {0};
        OrchestratorHarness harness = new OrchestratorHarness()
                .withTranslator((text, source, target, context, model) ->
                        Optional.of("Versuch " + (++calls[0])))
                .withValidator((original, translated, context) -> Optional.of(
                        ValidationReport.pass(Confidence.of(calls[0] == 1 ? 0.5 : 0.9))));
        harness.translate();

        assertThat(calls[0]).isEqualTo(2);
        assertThat(harness.onlyBlock().retryCount()).isEqualTo(1);
        assertThat(harness.onlyBlock().validationStatus()).isEqualTo(ValidationStatus.PASS);
    }

    @Test
    void exhaustsDepthBudgetIntoNeedsReview() {
        int[] calls = {0};
        OrchestratorHarness harness = new OrchestratorHarness()
                .withTranslator((text, source, target, context, model) -> {
                    calls[0]++;
                    return Optional.of("Nie akzeptiert");
                })
                .withValidator((original, translated, context) -> Optional.of(
                        ValidationReport.pass(Confidence.of(0.1))));
        harness.withRecursion(new RecursionSettings(2, 2, Confidence.of(0.85), true));
        harness.translate();

        assertThat(calls[0]).isEqualTo(3);
        assertThat(harness.onlyBlock().validationStatus()).isEqualTo(ValidationStatus.NEEDS_REVIEW);
    }

    @Test
    void emptyTranslationLandsInNeedsReviewWithoutRetry() {
        OrchestratorHarness harness = new OrchestratorHarness()
                .withTranslator((text, source, target, context, model) -> Optional.empty())
                .withValidator((original, translated, context) -> Optional.empty());
        harness.translate();

        assertThat(harness.onlyBlock().validationStatus()).isEqualTo(ValidationStatus.NEEDS_REVIEW);
        assertThat(harness.onlyBlock().retryCount()).isZero();
    }

    @Test
    void contextCarriesGlossaryAndSectionHeading() {
        String[] seenContexts = new String[2];
        OrchestratorHarness harness = new OrchestratorHarness()
                .withBlocks(block("b1", BlockContentType.HEADING, "Kapitel Eins"),
                        block("b2", BlockContentType.PARAGRAPH, "Text zwei"))
                .withGlossary(term("invoice", "Rechnung"))
                .withTranslator((text, source, target, context, model) -> {
                    seenContexts[0] = context.sectionHeading();
                    seenContexts[1] = context.glossaryTerms().isEmpty()
                            ? "empty"
                            : context.glossaryTerms().get(0).targetTerm();
                    return Optional.of("Übersetzung");
                })
                .withValidator((original, translated, context) ->
                        Optional.of(ValidationReport.pass(Confidence.of(1.0))));
        harness.translate();

        assertThat(seenContexts[0]).isEqualTo("Kapitel Eins");
        assertThat(seenContexts[1]).isEqualTo("Rechnung");
    }

    private static Block block(String id, BlockContentType type, String text) {
        return new Block(id, "p1", 0, type, text, new Position(0, 0, 10, 10),
                new FontInfo("Arial", 12f, FontStyle.REGULAR), 0, null,
                ValidationStatus.PENDING, null, 0, null, null, Instant.now(), Instant.now());
    }

    private static GlossaryTerm term(String source, String target) {
        return new GlossaryTerm("g1", "j1", source, target, "general", true, 1, Instant.now());
    }

    @Test
    void updatesJobAndPageCounters() {
        OrchestratorHarness harness = new OrchestratorHarness()
                .withTranslator((text, source, target, context, model) -> Optional.of("Hallo"))
                .withValidator((original, translated, context) ->
                        Optional.of(ValidationReport.pass(Confidence.of(1.0))));
        harness.translate();

        Page page = harness.page();
        Job job = harness.job();
        assertThat(page.status()).isEqualTo(PageStatus.COMPLETED);
        assertThat(job.completedBlocks()).isEqualTo(1);
        assertThat(job.totalBlocks()).isEqualTo(1);
    }

    private static class OrchestratorHarness {
        private final List<Block> blocks = new ArrayList<>();
        private final List<Block> saved = Collections.synchronizedList(new ArrayList<>());
        private final List<Page> pages = new ArrayList<>();
        private final List<Job> jobs = new ArrayList<>();
        private final List<GlossaryTerm> glossary = new ArrayList<>();
        private TranslationEngine translator =
                (text, source, target, context, model) -> Optional.of("Hallo");
        private SemanticValidator validator = (original, translated, context) ->
                Optional.of(ValidationReport.pass(Confidence.of(1.0)));
        private RecursionSettings recursion = RecursionSettings.defaults();

        OrchestratorHarness() {
            blocks.add(block("b1", BlockContentType.PARAGRAPH, "Hello world"));
            pages.add(new Page("p1", "j1", 1, PageStatus.PROCESSING, null, null, null,
                    0, null, Instant.now(), Instant.now()));
            jobs.add(new Job("j1", "nightly", "C:/docs/in.pdf", EN, DE, "llama3.1:8b",
                    null, JobStatus.PROCESSING, 0, 0, 0, 0, 0, 0, null,
                    Instant.now(), Instant.now()));
        }

        OrchestratorHarness withBlocks(Block... blocks) {
            this.blocks.clear();
            this.blocks.addAll(List.of(blocks));
            return this;
        }

        OrchestratorHarness withTranslator(TranslationEngine translator) {
            this.translator = translator;
            return this;
        }

        OrchestratorHarness withValidator(SemanticValidator validator) {
            this.validator = validator;
            return this;
        }

        OrchestratorHarness withRecursion(RecursionSettings recursion) {
            this.recursion = recursion;
            return this;
        }

        OrchestratorHarness withGlossary(GlossaryTerm term) {
            this.glossary.add(term);
            return this;
        }

        void translate() {
            ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
            ThroughputController throughput = new ThroughputController(
                    () -> new com.recursive.domain.HardwareSpec(32, 16, 8, null, 0, 200, true),
                    Duration.ofMillis(250));
            try {
                TranslationOrchestrator orchestrator = new TranslationOrchestrator(
                        translator, validator, blockRepository(), glossaryService());
                TranslationRunner runner = new TranslationRunner(
                        orchestrator, throughput, workers, blockRepository(),
                        pageRepository(), jobRepository());
                runner.translatePage("j1", "p1", EN, DE, "llama3.1:8b", recursion);
            } finally {
                throughput.close();
                workers.close();
            }
        }

        Block onlyBlock() {
            return saved.get(0);
        }

        Page page() {
            return pages.get(0);
        }

        Job job() {
            return jobs.get(0);
        }

        private BlockRepository blockRepository() {
            return new BlockRepository() {
                @Override
                public Block save(Block block) {
                    saved.add(block);
                    return block;
                }

                @Override
                public Optional<Block> findById(String id) {
                    return saved.stream().filter(b -> b.id().equals(id)).findFirst();
                }

                @Override
                public List<Block> findByPageId(String pageId) {
                    return deduplicated();
                }

                @Override
                public List<Block> findUnprocessedByPageId(String pageId) {
                    return deduplicated().stream()
                            .filter(b -> b.translatedText() == null).toList();
                }

                private List<Block> deduplicated() {
                    java.util.LinkedHashMap<String, Block> byId = new java.util.LinkedHashMap<>();
                    blocks.forEach(b -> byId.put(b.id(), b));
                    saved.forEach(b -> byId.put(b.id(), b));
                    return new ArrayList<>(byId.values());
                }

                @Override
                public void deleteByPageId(String pageId) {
                    saved.clear();
                }
            };
        }

        private PageRepository pageRepository() {
            return new PageRepository() {
                @Override
                public Page save(Page page) {
                    pages.set(0, page);
                    return page;
                }

                @Override
                public Optional<Page> findById(String id) {
                    return pages.stream().filter(p -> p.id().equals(id)).findFirst();
                }

                @Override
                public List<Page> findByJobId(String jobId) {
                    return new ArrayList<>(pages);
                }

                @Override
                public List<Page> findIncompleteByJobId(String jobId) {
                    return pages.stream().filter(p -> p.status() != PageStatus.COMPLETED).toList();
                }

                @Override
                public void deleteByJobId(String jobId) {
                    pages.clear();
                }
            };
        }

        private JobRepository jobRepository() {
            return new JobRepository() {
                @Override
                public Job save(Job job) {
                    jobs.set(0, job);
                    return job;
                }

                @Override
                public Optional<Job> findById(String id) {
                    return jobs.stream().filter(j -> j.id().equals(id)).findFirst();
                }

                @Override
                public List<Job> findAll() {
                    return new ArrayList<>(jobs);
                }

                @Override
                public List<Job> findByStatus(JobStatus status) {
                    return jobs.stream().filter(j -> j.status() == status).toList();
                }

                @Override
                public void delete(String id) {
                    jobs.clear();
                }
            };
        }

        private CachingGlossaryService glossaryService() {
            return new CachingGlossaryService(new GlossaryRepository() {
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
                    return new ArrayList<>(glossary);
                }

                @Override
                public List<GlossaryTerm> findLockedByJobId(String jobId) {
                    return new ArrayList<>(glossary);
                }

                @Override
                public void deleteByJobId(String jobId) {
                    glossary.clear();
                }
            });
        }
    }
}
