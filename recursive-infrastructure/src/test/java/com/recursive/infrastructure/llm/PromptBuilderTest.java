package com.recursive.infrastructure.llm;

import com.recursive.domain.GlossaryTerm;
import com.recursive.domain.Language;
import com.recursive.domain.ProcessingContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptBuilderTest {

    private final Language english = Language.of("en", "English");
    private final Language german = Language.of("de", "Deutsch");

    @Test
    void includesLanguagesAndText() {
        String prompt = PromptBuilder.translate("Hello world", english.code(), german.code(),
                ProcessingContext.empty());

        assertThat(prompt)
                .contains("from en to de")
                .contains("Hello world")
                .contains("Output only the translation");
    }

    @Test
    void includesContextAndGlossary() {
        GlossaryTerm term = new GlossaryTerm("g1", "j1", "invoice", "Rechnung", "finance",
                true, 3, Instant.now());
        ProcessingContext context = new ProcessingContext("Previous line", "Next line",
                "Chapter 1", List.of(term));

        String prompt = PromptBuilder.translate("The invoice arrived", english.code(),
                german.code(), context);

        assertThat(prompt)
                .contains("Previous line")
                .contains("Next line")
                .contains("Chapter 1")
                .contains("invoice -> Rechnung");
    }
}
