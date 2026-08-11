package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.Confidence;
import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.PageStatus;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.SemanticValidator;
import com.recursive.domain.TranslationEngine;
import com.recursive.domain.ValidationReport;
import com.recursive.domain.ValidationStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Runs the recursive translation loop over the blocks of one page: each
 * block is translated, validated, and re-translated until the recursion
 * settings accept it or the depth budget is exhausted. Stateless by design;
 * one instance is safe to share across workers.
 */
public class TranslationOrchestrator {

    private final TranslationEngine translator;
    private final SemanticValidator validator;
    private final BlockRepository blockRepository;
    private final PageRepository pageRepository;
    private final JobRepository jobRepository;
    private final CachingGlossaryService glossaryService;

    public TranslationOrchestrator(TranslationEngine translator, SemanticValidator validator,
                                   BlockRepository blockRepository, PageRepository pageRepository,
                                   JobRepository jobRepository, CachingGlossaryService glossaryService) {
        this.translator = translator;
        this.validator = validator;
        this.blockRepository = blockRepository;
        this.pageRepository = pageRepository;
        this.jobRepository = jobRepository;
        this.glossaryService = glossaryService;
    }

    public void translatePage(String jobId, String pageId, Language source, Language target,
                              String modelName, RecursionSettings recursion) {
        List<Block> blocks = inReadingOrder(blockRepository.findUnprocessedByPageId(pageId));
        if (blocks.isEmpty()) {
            return;
        }
        String sectionHeading = null;
        for (int i = 0; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            if (block.contentType() == BlockContentType.HEADING
                    || block.contentType() == BlockContentType.SUBHEADING) {
                sectionHeading = block.originalText();
            }
            ProcessingContext context = new ProcessingContext(
                    previousText(blocks, i), nextText(blocks, i), sectionHeading,
                    glossaryService.lockedTermsFor(jobId));
            translateBlock(block, context, source, target, modelName, recursion);
        }
        updateCounters(jobId, pageId, blocks.size());
    }

    private void translateBlock(Block block, ProcessingContext context, Language source,
                                Language target, String modelName, RecursionSettings recursion) {
        int passes = 0;
        while (true) {
            Optional<String> translation = translator.translate(block.originalText(), source,
                    target, context, modelName);
            if (translation.isEmpty()) {
                block.setValidationStatus(ValidationStatus.NEEDS_REVIEW);
                blockRepository.save(block);
                return;
            }
            ValidationReport verdict = validator.validate(block.originalText(), translation.get(), context)
                    .orElseGet(() -> ValidationReport.pass(Confidence.of(1.0)));
            block.setTranslatedText(translation.get());
            block.setConfidenceScore(verdict.confidenceScore().score());
            passes++;
            if (recursion.accepts(verdict.confidenceScore(), passes)) {
                block.setValidationStatus(ValidationStatus.PASS);
                blockRepository.save(block);
                return;
            }
            if (block.retryCount() >= recursion.maxDepth()) {
                block.setValidationStatus(ValidationStatus.NEEDS_REVIEW);
                blockRepository.save(block);
                return;
            }
            block.setRetryCount(block.retryCount() + 1);
            blockRepository.save(block);
        }
    }

    private void updateCounters(String jobId, String pageId, int pageBlockCount) {
        Page page = pageRepository.findById(pageId)
                .orElseThrow(() -> new IllegalStateException("Unknown page: " + pageId));
        page.setStatus(PageStatus.COMPLETED);
        pageRepository.save(page);

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Unknown job: " + jobId));
        List<Page> pages = pageRepository.findByJobId(jobId);
        int completedPages = (int) pages.stream().filter(p -> p.status() == PageStatus.COMPLETED).count();
        List<Block> allBlocks = new ArrayList<>();
        for (Page p : pages) {
            allBlocks.addAll(blockRepository.findByPageId(p.id()));
        }
        long done = allBlocks.stream().filter(b -> b.translatedText() != null).count();
        long failed = allBlocks.stream()
                .filter(b -> b.validationStatus() == ValidationStatus.NEEDS_REVIEW).count();
        job.setPageCounters(pages.size(), completedPages);
        job.setBlockCounters(allBlocks.size(), (int) done, (int) (done - failed), (int) failed);
        jobRepository.save(job);
    }

    private static List<Block> inReadingOrder(List<Block> blocks) {
        List<Block> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt(Block::readingOrder));
        return sorted;
    }

    private static String previousText(List<Block> blocks, int index) {
        return index > 0 ? blocks.get(index - 1).translatedText() : null;
    }

    private static String nextText(List<Block> blocks, int index) {
        return index < blocks.size() - 1 ? blocks.get(index + 1).originalText() : null;
    }
}
