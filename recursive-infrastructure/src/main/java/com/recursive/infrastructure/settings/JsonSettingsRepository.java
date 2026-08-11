package com.recursive.infrastructure.settings;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Jackson-based persistence of application settings as a single JSON file.
 * Missing files yield defaults; corrupt files are rejected loudly rather
 * than silently reset, so the user learns the file is broken.
 */
public class JsonSettingsRepository<T> {

    private final ObjectMapper objectMapper;
    private final Path settingsFile;
    private final Class<T> settingsType;

    public JsonSettingsRepository(ObjectMapper objectMapper, Path settingsFile, Class<T> settingsType) {
        this.objectMapper = objectMapper;
        this.settingsFile = settingsFile;
        this.settingsType = settingsType;
    }

    public T load(T defaults) {
        if (!Files.exists(settingsFile)) {
            return defaults;
        }
        try {
            return objectMapper.readValue(settingsFile.toFile(), settingsType);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read settings file: " + settingsFile, e);
        }
    }

    public void save(T settings) {
        try {
            Files.createDirectories(settingsFile.getParent());
            objectMapper.writeValue(settingsFile.toFile(), settings);
        } catch (IOException e) {
            throw new IllegalStateException("Could not write settings file: " + settingsFile, e);
        }
    }
}
