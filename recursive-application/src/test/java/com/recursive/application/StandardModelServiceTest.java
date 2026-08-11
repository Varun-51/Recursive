package com.recursive.application;

import com.recursive.domain.Confidence;
import com.recursive.domain.Language;
import com.recursive.domain.ProcessingContext;
import com.recursive.domain.TranslationEngine;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardModelServiceTest {

    @Test
    void delegatesAndReturnsTranslation() {
        StandardModelService service = new StandardModelService(
                (text, source, target, context, model) -> Optional.of("Bonjour"));
        Optional<String> result = service.translate("Hello", Language.of("en", "English"),
                Language.of("fr", "Français"), ProcessingContext.empty(), "llama3.1:8b");

        assertThat(result).contains("Bonjour");
    }

    @Test
    void rejectsBlankModelName() {
        StandardModelService service = new StandardModelService(
                (text, source, target, context, model) -> Optional.empty());
        assertThatThrownBy(() -> service.translate("Hello", Language.of("en", "English"),
                Language.of("fr", "Français"), ProcessingContext.empty(), " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}