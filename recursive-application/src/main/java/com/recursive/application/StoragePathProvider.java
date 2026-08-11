package com.recursive.application;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the application's on-disk locations: the data root, per-job
 * output, development scratch space, and analysis exports. All paths derive
 * from the user home so nothing lands inside the installation directory.
 */
public class StoragePathProvider {

    private static final String APP_DIR_NAME = ".recursive";

    public Path dataRoot() {
        return Paths.get(System.getProperty("user.home"), APP_DIR_NAME);
    }

    public Path devRoot() {
        return dataRoot().resolve("dev");
    }

    public Path analysisRoot() {
        return dataRoot().resolve("analysis");
    }

    public Path jobOutput(String jobId) {
        return dataRoot().resolve("output").resolve(validSegment(jobId));
    }

    private static String validSegment(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid path segment: " + value);
        }
        return value;
    }
}
