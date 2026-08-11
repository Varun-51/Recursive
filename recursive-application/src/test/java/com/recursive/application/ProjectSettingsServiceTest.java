package com.recursive.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectSettingsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultsToUserDocuments() {
        ProjectSettingsService service = new ProjectSettingsService();
        assertThat(service.defaultInputFolder()).isEqualTo(Paths.get(System.getProperty("user.home"), "Documents"));
        assertThat(service.autoSaveEnabled()).isTrue();
    }

    @Test
    void updateAcceptsExistingDirectories() throws Exception {
        Path input = Files.createDirectory(tempDir.resolve("in"));
        Path output = Files.createDirectory(tempDir.resolve("out"));
        ProjectSettingsService service = new ProjectSettingsService();

        service.update(input, output, false);

        assertThat(service.defaultInputFolder()).isEqualTo(input.toAbsolutePath().normalize());
        assertThat(service.autoSaveEnabled()).isFalse();
    }

    @Test
    void updateRejectsMissingDirectories() {
        ProjectSettingsService service = new ProjectSettingsService();
        assertThatThrownBy(() -> service.update(tempDir.resolve("nope"),
                tempDir, true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(tempDir, null, true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
