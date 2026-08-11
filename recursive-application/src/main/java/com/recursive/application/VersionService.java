package com.recursive.application;

/**
 * Versioning facts surfaced to the UI: the application release, the model
 * snapshot a job ran against, and the build of the developer toolchain.
 */
public class VersionService {

    private final String appVersion;
    private final String modelVersion;
    private final String developerVersion;

    public VersionService(String appVersion, String modelVersion, String developerVersion) {
        if (appVersion == null || appVersion.isBlank()
                || modelVersion == null || modelVersion.isBlank()
                || developerVersion == null || developerVersion.isBlank()) {
            throw new IllegalArgumentException("Versions must not be blank");
        }
        this.appVersion = appVersion;
        this.modelVersion = modelVersion;
        this.developerVersion = developerVersion;
    }

    public static VersionService defaults() {
        return new VersionService("0.1.0", "unknown", "unknown");
    }

    public String appVersion() {
        return appVersion;
    }

    public String modelVersion() {
        return modelVersion;
    }

    public String developerVersion() {
        return developerVersion;
    }
}
