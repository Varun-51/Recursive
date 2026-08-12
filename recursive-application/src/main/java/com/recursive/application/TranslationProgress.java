package com.recursive.application;

/**
 * Snapshot of a translate-all run, delivered from worker threads as pages
 * complete. Counts are pages, not blocks: block totals are already tracked
 * on the {@link com.recursive.domain.Job} row.
 */
public record TranslationProgress(int completedPages, int totalPages) {

    public double fraction() {
        return totalPages == 0 ? 1.0 : (double) completedPages / totalPages;
    }
}
