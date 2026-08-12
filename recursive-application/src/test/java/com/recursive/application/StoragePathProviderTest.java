package com.recursive.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePathProviderTest {

    private final StoragePathProvider provider = new StoragePathProvider();

    @Test
    void dataRootLivesInUserHome() {
        assertThat(provider.dataRoot().toString())
                .startsWith(System.getProperty("user.home"))
                .endsWith(".recursive");
    }

    @Test
    void derivedRootsAreNested() {
        assertThat(provider.devRoot()).isEqualTo(provider.dataRoot().resolve("dev"));
        assertThat(provider.analysisRoot()).isEqualTo(provider.dataRoot().resolve("analysis"));
    }

    @Test
    void jobOutputIsScopedUnderDataRoot() {
        assertThat(provider.jobOutput("job-42"))
                .isEqualTo(provider.dataRoot().resolve("output").resolve("job-42"));
    }

    @Test
    void rejectsPathTraversalInJobId() {
        assertThatThrownBy(() -> provider.jobOutput("../evil"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.jobOutput("a/b"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> provider.jobOutput(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
