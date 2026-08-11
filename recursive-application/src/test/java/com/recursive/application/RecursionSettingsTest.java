package com.recursive.application;

import com.recursive.domain.Confidence;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecursionSettingsTest {

    @Test
    void rejectsInvalidConstruction() {
        assertThatThrownBy(() -> new RecursionSettings(0, 2, Confidence.of(0.85), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecursionSettings(3, 0, Confidence.of(0.85), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RecursionSettings(3, 2, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsWhenStablePassesAndConfidenceMet() {
        RecursionSettings settings = new RecursionSettings(3, 2, Confidence.of(0.85), true);
        assertThat(settings.accepts(Confidence.of(0.9), 2)).isTrue();
        assertThat(settings.accepts(Confidence.of(0.9), 1)).isFalse();
        assertThat(settings.accepts(Confidence.of(0.8), 2)).isFalse();
    }

    @Test
    void disabledRecursionAcceptsFirstPass() {
        RecursionSettings settings = new RecursionSettings(3, 2, Confidence.of(0.85), false);
        assertThat(settings.accepts(Confidence.of(0.0), 1)).isTrue();
    }

    @Test
    void defaultsAreSane() {
        RecursionSettings defaults = RecursionSettings.defaults();
        assertThat(defaults.maxDepth()).isEqualTo(3);
        assertThat(defaults.minStablePasses()).isEqualTo(2);
        assertThat(defaults.confidenceThreshold().score()).isEqualTo(0.85);
        assertThat(defaults.enabled()).isTrue();
    }
}