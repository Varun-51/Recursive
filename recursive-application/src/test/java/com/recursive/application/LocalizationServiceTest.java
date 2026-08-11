package com.recursive.application;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalizationServiceTest {

    @Test
    void exposesAppNameAndLocale() {
        LocalizationService service = new LocalizationService("Recursive", Locale.GERMAN);

        assertThat(service.appName()).isEqualTo("Recursive");
        assertThat(service.locale()).isEqualTo(Locale.GERMAN);
    }

    @Test
    void unknownKeysDegradeToTheKeyItself() {
        LocalizationService service = LocalizationService.defaults();
        assertThat(service.text("settings.title")).isEqualTo("settings.title");
    }

    @Test
    void rejectsBlankAppNameAndKey() {
        assertThatThrownBy(() -> new LocalizationService(" ", Locale.ENGLISH))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> LocalizationService.defaults().text(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}