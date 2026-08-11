package com.recursive.domain;

import java.util.Locale;

/**
 * Model confidence on a 0.0-1.0 scale. Factory methods keep the unit in one
 * place so thresholds are always compared on the same bounds.
 */
public record Confidence(double score) {

    public Confidence {
        if (Double.isNaN(score) || score < 0.0 || score > 1.0) {
            throw new IllegalArgumentException("Confidence must be in [0.0, 1.0]");
        }
    }

    public static Confidence of(double score) {
        return new Confidence(score);
    }

    public static Confidence fromPercent(double percentage) {
        return new Confidence(percentage / 100.0);
    }

    public double asPercent() {
        return score * 100.0;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.1f%%", asPercent());
    }
}