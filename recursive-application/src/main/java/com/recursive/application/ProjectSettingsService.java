package com.recursive.application;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * User-level project settings: where documents come from and go to, and
 * whether intermediate state is saved. Values are validated at the boundary
 * so the UI never sees inconsistent folders.
 */
public class ProjectSettingsService {

    private Path defaultInputFolder = Paths.get(System.getProperty("user.home"), "Documents");
    private Path defaultOutputFolder = Paths.get(System.getProperty("user.home"), "Documents");
    private boolean autoSaveEnabled = true;

    public Path defaultInputFolder() {
        return defaultInputFolder;
    }

    public Path defaultOutputFolder() {
        return defaultOutputFolder;
    }

    public boolean autoSaveEnabled() {
        return autoSaveEnabled;
    }

    public void update(Path defaultInputFolder, Path defaultOutputFolder, boolean autoSaveEnabled) {
        this.defaultInputFolder = validFolder(defaultInputFolder, "defaultInputFolder");
        this.defaultOutputFolder = validFolder(defaultOutputFolder, "defaultOutputFolder");
        this.autoSaveEnabled = autoSaveEnabled;
    }

    private static Path validFolder(Path folder, String fieldName) {
        if (folder == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        Path normalized = folder.toAbsolutePath().normalize();
        if (!normalized.toFile().isDirectory()) {
            throw new IllegalArgumentException(fieldName + " is not a directory: " + normalized);
        }
        return normalized;
    }
}
