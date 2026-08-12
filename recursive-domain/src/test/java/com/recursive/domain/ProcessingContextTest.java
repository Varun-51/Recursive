package com.recursive.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProcessingContextTest {

    @Test
    void rejectsNullGlossary() {
        assertThatThrownBy(() -> new ProcessingContext("prev", "next", "Section", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyContextIsUsable() {
        ProcessingContext empty = ProcessingContext.empty();
        assertThat(empty.previousBlockText()).isNull();
        assertThat(empty.sectionHeading()).isNull();
        assertThat(empty.glossaryTerms()).isEmpty();
    }

    @Test
    void carriesGlossaryIntoTranslationScope() {
        GlossaryTerm term = new GlossaryTerm("g1", "j1", "invoice", "Rechnung", "finance", true, 3, null);
        ProcessingContext context = new ProcessingContext("prev", null, "Accounting", List.of(term));
        assertThat(context.glossaryTerms()).containsExactly(term);
    }
}
