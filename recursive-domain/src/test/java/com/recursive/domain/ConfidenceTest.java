package com.recursive.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfidenceTest {

    @Test
    void acceptsInclusiveBounds() {
        assertThat(Confidence.of(0.0).score()).isZero();
        assertThat(Confidence.of(1.0).score()).isEqualTo(1.0);
    }

    @Test
    void rejectsOutOfRangeScores() {
        assertThatThrownBy(() -> Confidence.of(-0.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Confidence.of(1.01)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Confidence.of(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertsBetweenPercentAndFraction() {
        assertThat(Confidence.fromPercent(87.5).score()).isEqualTo(0.875);
        assertThat(Confidence.of(0.25).asPercent()).isEqualTo(25.0);
    }

    @Test
    void rendersAsPercent() {
        assertThat(Confidence.of(0.875).toString()).isEqualTo("87.5%");
    }
}