package com.recursive.infrastructure.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonSettingsRepositoryTest {

    @TempDir
    Path tempDir;

    private record TestSettings(String language, int parallelJobs) {
    }

    private final TestSettings defaults = new TestSettings("en", 2);

    @Test
    void returnsDefaultsWhenNoFileExists() {
        JsonSettingsRepository<TestSettings> repository =
                new JsonSettingsRepository<>(new ObjectMapper(), tempDir.resolve("missing.json"), TestSettings.class);

        assertThat(repository.load(defaults)).isEqualTo(defaults);
    }

    @Test
    void roundTripsThroughJson() throws IOException {
        Path settingsFile = tempDir.resolve("settings.json");
        JsonSettingsRepository<TestSettings> repository =
                new JsonSettingsRepository<>(new ObjectMapper(), settingsFile, TestSettings.class);
        repository.save(new TestSettings("de", 4));

        TestSettings loaded = repository.load(defaults);

        assertThat(loaded).isEqualTo(new TestSettings("de", 4));
        assertThat(Files.readString(settingsFile)).contains("\"language\"", "\"de\"");
    }

    @Test
    void rejectsCorruptFileLoudly() throws IOException {
        Path settingsFile = tempDir.resolve("settings.json");
        Files.writeString(settingsFile, "{ not json");
        JsonSettingsRepository<TestSettings> repository =
                new JsonSettingsRepository<>(new ObjectMapper(), settingsFile, TestSettings.class);

        assertThatThrownBy(() -> repository.load(defaults))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("settings file");
    }
}
