package com.recursive.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void languageRejectsBlankCode() {
        assertThatThrownBy(() -> Language.of("  ", "English"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Language.of(null, "English"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void languageAcceptsValidPair() {
        Language english = Language.of("en", "English");
        assertThat(english.code()).isEqualTo("en");
        assertThat(english.name()).isEqualTo("English");
    }

    @Test
    void positionRejectsNegativeDimensions() {
        assertThatThrownBy(() -> new Position(10, 10, -1, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Position(10, 10, 5, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void positionAcceptsZeroSizedPlacement() {
        assertThat(new Position(0, 0, 0, 0).width()).isZero();
    }

    @Test
    void fontInfoValidatesAllFields() {
        assertThatThrownBy(() -> new FontInfo(" ", 12f, FontStyle.REGULAR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FontInfo("Helvetica", 0f, FontStyle.REGULAR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FontInfo("Helvetica", 12f, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void confidenceIsThreatenedByNothingButBounds() {
        assertThat(Confidence.of(0.5).asPercent()).isEqualTo(50.0);
    }
}
