package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.Confidence;
import com.recursive.domain.Language;
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
 * The recursive translation loop for one block: translate, validate, and
 * re-translate until the recursion settings accept the result or the depth
 * budget is exhausted. Stateless and thread-safe — worker threads of the
 * {@link TranslationRunner} call {@link #translateBlockWithContext}
 * concurrently. Neighbour context is read fresh from the repository, so a
 * task never mutates a {@link Block} instance another task holds; a block
 * whose neighbours already finished simply sees their finished
 * translations.
 */
public class TranslationOrchestrator {

    private final TranslationEngine translator;
    private final SemanticValidator validator;
    private final BlockRepository blockRepository;
    private final CachingGlossaryService glossaryService;

    public TranslationOrchestrator(TranslationEngine translator, SemanticValidator validator,
                                   BlockRepository blockRepository,
                                   CachingGlossaryService glossaryService) {
        this.translator = translator;
        this.validator = validator;
        this.blockRepository = blockRepository;
        this.glossaryService = glossaryService;
    }

    /** Block ids of {@code pageId} that have no translation yet, in reading order. */
    public List<String> pendingBlockIds(String pageId) {
        return inReadingOrder(blockRepository.findUnprocessedByPageId(pageId))
                .stream().map(Block::id).toList();
    }

    /** Runs the recursion loop for one block, saving every state change. */
    public void translateBlockWithContext(BlockCoordinates where,
                                          Language source, Language target, String modelName,
                                          RecursionSettings recursion) {
        List<Block> current = inReadingOrder(blockRepository.findByPageId(where.pageId()));
        int index = indexOf(current, where.blockId());
        Block block = current.get(index);
        ProcessingContext context = new ProcessingContext(
                previousText(current, index), nextText(current, index),
                sectionHeading(current, index), glossaryService.lockedTermsFor(where.jobId()));
        translateBlock(block, context, source, target, modelName, recursion);
    }

    /** Marks a block for human review, e.g. after an interrupted or failed task. */
    public void markForReview(String blockId) {
        blockRepository.findById(blockId).ifPresent(block -> {
            block.setValidationStatus(ValidationStatus.NEEDS_REVIEW);
            blockRepository.save(block);
        });
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

    private static List<Block> inReadingOrder(List<Block> blocks) {
        List<Block> sorted = new ArrayList<>(blocks);
        sorted.sort(Comparator.comparingInt(Block::readingOrder));
        return sorted;
    }

    private static int indexOf(List<Block> blocks, String id) {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).id().equals(id)) {
                return i;
            }
        }
        throw new IllegalStateException("Block vanished from page between reads: " + id);
    }

    private static String sectionHeading(List<Block> blocks, int index) {
        for (int i = index; i >= 0; i--) {
            BlockContentType type = blocks.get(i).contentType();
            if (type == BlockContentType.HEADING || type == BlockContentType.SUBHEADING) {
                return blocks.get(i).originalText();
            }
        }
        return null;
    }

    private static String previousText(List<Block> blocks, int index) {
        return index > 0 ? blocks.get(index - 1).translatedText() : null;
    }

    private static String nextText(List<Block> blocks, int index) {
        return index < blocks.size() - 1 ? blocks.get(index + 1).originalText() : null;
    }
}
