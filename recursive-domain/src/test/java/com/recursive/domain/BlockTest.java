package com.recursive.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BlockTest {

    @Test
    void rejectsMissingRequiredState() {
        assertThatThrownBy(() -> blockWithId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> blockWithPageId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> blockWithPageId("")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tracksVerificationStateTransitions() {
        Block block = blockWithId("b1");
        block.setTranslatedText("Bonjour le monde");
        block.setValidationStatus(ValidationStatus.PASS);
        block.setConfidenceScore(0.91);
        assertThat(block.translatedText()).isEqualTo("Bonjour le monde");
        assertThat(block.validationStatus()).isEqualTo(ValidationStatus.PASS);
        assertThat(block.confidenceScore()).isEqualTo(0.91);
    }

    @Test
    void validationReportPassFactoryCarriesConfidence() {
        ValidationReport report = ValidationReport.pass(Confidence.of(0.99));
        assertThat(report.status()).isEqualTo(ValidationStatus.PASS);
        assertThat(report.issues()).isEmpty();
    }

    private static Block blockWithId(String id) {
        return new Block(id, "p1", 0, BlockContentType.PARAGRAPH, "Hello world",
                new Position(10, 20, 300, 12), new FontInfo("Helvetica", 12f, FontStyle.REGULAR),
                0, null, ValidationStatus.PENDING, null, 0, null, null, Instant.now(), Instant.now());
    }

    private static Block blockWithPageId(String pageId) {
        return new Block("b1", pageId, 0, BlockContentType.PARAGRAPH, "Hello world",
                new Position(10, 20, 300, 12), new FontInfo("Helvetica", 12f, FontStyle.REGULAR),
                0, null, ValidationStatus.PENDING, null, 0, null, null, Instant.now(), Instant.now());
    }
}