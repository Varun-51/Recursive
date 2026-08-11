package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.Confidence;
import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageStatus;
import com.recursive.domain.ValidationStatus;

import java.time.Instant;
import java.util.List;

/**
 * Translates domain entities into immutable view DTOs so the JavaFX layer
 * never touches domain objects. Nullable fields stay null; confidence is
 * delivered as a display percentage.
 */
public class DTOMapper {

    public record JobView(String id, String name, String sourceFilePath,
                          Language sourceLanguage, Language targetLanguage,
                          String modelName, JobStatus status,
                          int totalPages, int completedPages,
                          int totalBlocks, int completedBlocks,
                          int validatedBlocks, int failedBlocks,
                          String errorMessage, Instant updatedAt) {
    }

    public record BlockView(String id, String pageId, int blockIndex,
                            BlockContentType contentType, String originalText,
                            String translatedText, ValidationStatus validationStatus,
                            Double confidencePercent, int retryCount) {
    }

    public record PageView(String id, int pageNumber, PageStatus status,
                           int retryCount, String errorMessage, Instant updatedAt) {
    }

    public JobView toJobView(Job job) {
        if (job == null) {
            return null;
        }
        return new JobView(job.id(), job.name(), job.sourceFilePath(),
                job.sourceLanguage(), job.targetLanguage(), job.modelName(), job.status(),
                job.totalPages(), job.completedPages(), job.totalBlocks(), job.completedBlocks(),
                job.validatedBlocks(), job.failedBlocks(), job.errorMessage(), job.updatedAt());
    }

    public List<JobView> toJobViews(List<Job> jobs) {
        return jobs.stream().map(this::toJobView).toList();
    }

    public BlockView toBlockView(Block block) {
        if (block == null) {
            return null;
        }
        return new BlockView(block.id(), block.pageId(), block.blockIndex(),
                block.contentType(), block.originalText(), block.translatedText(),
                block.validationStatus(), toPercent(block.confidenceScore()), block.retryCount());
    }

    public List<BlockView> toBlockViews(List<Block> blocks) {
        return blocks.stream().map(this::toBlockView).toList();
    }

    public PageView toPageView(Page page) {
        if (page == null) {
            return null;
        }
        return new PageView(page.id(), page.pageNumber(), page.status(),
                page.retryCount(), page.errorMessage(), page.updatedAt());
    }

    public List<PageView> toPageViews(List<Page> pages) {
        return pages.stream().map(this::toPageView).toList();
    }

    private static Double toPercent(Double score) {
        return score == null ? null : Confidence.of(score).asPercent();
    }
}
