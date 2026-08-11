package com.recursive.infrastructure.verification;

import com.recursive.domain.Confidence;
import com.recursive.domain.GlossaryTerm;
import com.recursive.domain.Language;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.ValidationReport;
import com.recursive.domain.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AdaptiveFlowValidatorTest {

    private final AdaptiveFlowValidator validator = new AdaptiveFlowValidator();

    @Test
    void passesCleanTranslation() {
        Optional<ValidationReport> report = validator.validate(
                "The price is 42 dollars.", "Der Preis beträgt 42 Dollar.",
                ProcessingContext.empty());

        assertThat(report).isEmpty();
    }

    @Test
    void flagsMissingNumber() {
        Optional<ValidationReport> report = validator.validate(
                "The price is 42 dollars.", "Der Preis beträgt Dollar.",
                ProcessingContext.empty());

        assertThat(report).isPresent();
        assertThat(report.get().status()).isEqualTo(ValidationStatus.NEEDS_REVIEW);
        assertThat(report.get().issues())
                .anyMatch(issue -> issue.contains("42"));
    }

    @Test
    void flagsDroppedNegation() {
        Optional<ValidationReport> report = validator.validate(
                "The device does not work.", "Das Gerät funktioniert.",
                ProcessingContext.empty());

        assertThat(report).isPresent();
        assertThat(report.get().issues())
                .anyMatch(issue -> issue.contains("negation"));
    }

    @Test
    void flagsBrokenLockedTerm() {
        GlossaryTerm term = new GlossaryTerm("g1", "j1", "invoice", "Rechnung", "finance",
                true, 1, Instant.now());
        ProcessingContext context = new ProcessingContext(null, null, null, List.of(term));

        Optional<ValidationReport> report = validator.validate(
                "The invoice was sent.", "Die Rechnung wurde gesendet.",
                context);
        Optional<ValidationReport> broken = validator.validate(
                "The invoice was sent.", "Das Dokument wurde gesendet.",
                context);

        assertThat(report).isEmpty();
        assertThat(broken).isPresent();
        assertThat(broken.get().issues())
                .anyMatch(issue -> issue.contains("Rechnung"));
    }
}
