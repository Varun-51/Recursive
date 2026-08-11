package com.recursive.infrastructure.appconfig;

import com.recursive.application.StoragePathProvider;

import java.nio.file.Path;

/**
 * Application-wide configuration resolved once at startup. Everything
 * derives from the user-home data root so no file lands in the install
 * directory.
 *
 * @param databaseFile   SQLite database location
 * @param ollamaBaseUrl  local Ollama HTTP endpoint
 * @param storagePaths   on-disk location policy
 */
public record AppConfig(Path databaseFile, String ollamaBaseUrl, StoragePathProvider storagePaths) {

    public static AppConfig fromDefaults() {
        StoragePathProvider storagePaths = new StoragePathProvider();
        return new AppConfig(
                storagePaths.dataRoot().resolve("recursive.db"),
                "http://localhost:11434",
                storagePaths);
    }
}
