package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.Job;
import com.recursive.domain.JobRepository;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.PageStatus;
import com.recursive.domain.ValidationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * Schedules translation work: blocks of a page and pages of a job run in
 * parallel on the injected virtual-thread pool, throttled by the
 * {@link ThroughputController} so the machine is never overloaded. After a
 * page completes, the job counters are recomputed — serialized so
 * concurrent completions never lose each other's updates. Thread-safe.
 */
public class TranslationRunner {

    private static final Logger log = LoggerFactory.getLogger(TranslationRunner.class);

    private final TranslationOrchestrator orchestrator;
    private final ThroughputController throughput;
    private final ExecutorService workers;
    private final BlockRepository blockRepository;
    private final PageRepository pageRepository;
    private final JobRepository jobRepository;

    public TranslationRunner(TranslationOrchestrator orchestrator, ThroughputController throughput,
                             ExecutorService workers, BlockRepository blockRepository,
                             PageRepository pageRepository, JobRepository jobRepository) {
        this.orchestrator = orchestrator;
        this.throughput = throughput;
        this.workers = workers;
        this.blockRepository = blockRepository;
        this.pageRepository = pageRepository;
        this.jobRepository = jobRepository;
    }

    public void translatePage(String jobId, String pageId, Language source, Language target,
                              String modelName, RecursionSettings recursion) {
        List<String> blockIds = orchestrator.pendingBlockIds(pageId);
        if (blockIds.isEmpty()) {
            return;
        }
        CountDownLatch pageLatch = new CountDownLatch(blockIds.size());
        for (String blockId : blockIds) {
            workers.execute(() -> translateBlock(
                    new BlockCoordinates(blockId, pageId, jobId),
                    source, target, modelName, recursion, pageLatch));
        }
        awaitQuietly(pageLatch);
        updateCounters(jobId, pageId);
    }

    /**
     * Translates every pending page of a job on the worker pool; {@code
     * progress} receives a snapshot after each page (called from worker
     * threads). Returns when all pages finished or were skipped.
     */
    public void translateJob(String jobId, Language source, Language target,
                             String modelName, RecursionSettings recursion,
                             Consumer<TranslationProgress> progress) {
        List<Page> pages = pageRepository.findByJobId(jobId);
        List<Page> pending = pages.stream()
                .filter(page -> page.status() != PageStatus.COMPLETED)
                .toList();
        if (pending.isEmpty()) {
            progress.accept(new TranslationProgress(pages.size(), pages.size()));
            return;
        }
        CountDownLatch jobLatch = new CountDownLatch(pending.size());
        int[] completed = {0};
        for (Page page : pending) {
            workers.execute(() -> {
                try {
                    translatePage(jobId, page.id(), source, target, modelName, recursion);
                } catch (RuntimeException e) {
                    log.error("Page {} of job {} failed", page.id(), jobId, e);
                    markPageFailed(jobId, page.id());
                } finally {
                    int done;
                    synchronized (completed) {
                        done = ++completed[0];
                    }
                    progress.accept(new TranslationProgress(done, pages.size()));
                    jobLatch.countDown();
                }
            });
        }
        awaitQuietly(jobLatch);
    }

    private void translateBlock(BlockCoordinates where, Language source, Language target,
                                String modelName, RecursionSettings recursion,
                                CountDownLatch pageLatch) {
        boolean acquired = false;
        try {
            throughput.acquire();
            acquired = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            if (acquired) {
                orchestrator.translateBlockWithContext(where, source, target, modelName, recursion);
            } else {
                orchestrator.markForReview(where.blockId());
            }
        } catch (RuntimeException e) {
            log.error("Block {} of page {} failed", where.blockId(), where.pageId(), e);
            orchestrator.markForReview(where.blockId());
        } finally {
            if (acquired) {
                throughput.release();
            }
            pageLatch.countDown();
        }
    }

    private void markPageFailed(String jobId, String pageId) {
        pageRepository.findById(pageId).ifPresent(page -> {
            page.setStatus(PageStatus.FAILED);
            pageRepository.save(page);
        });
        updateCounters(jobId, pageId);
    }

    /** Serialized so concurrent page completions never lose each other's counter updates. */
    private synchronized void updateCounters(String jobId, String pageId) {
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

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Translation interrupted while waiting for workers", e);
        }
    }
}
