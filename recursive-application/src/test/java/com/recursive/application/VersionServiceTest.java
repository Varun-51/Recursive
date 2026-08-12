package com.recursive.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VersionServiceTest {

    @Test
    void exposesAllThreeVersions() {
        VersionService service = new VersionService("1.2.3", "llama3.1:8b", "2026-01-01");

        assertThat(service.appVersion()).isEqualTo("1.2.3");
        assertThat(service.modelVersion()).isEqualTo("llama3.1:8b");
        assertThat(service.developerVersion()).isEqualTo("2026-01-01");
    }

    @Test
    void rejectsBlankValues() {
        assertThatThrownBy(() -> new VersionService(" ", "x", "y"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
