package com.recursive.app;

import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.infrastructure.appconfig.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CompositionRootTest {

    @TempDir
    Path tempDir;

    @Test
    void wiresAllServicesAndRunsJobLifecycle() throws IOException {
        Path sourcePdf = tempDir.resolve("source.pdf");
        Files.writeString(sourcePdf, "not a real pdf");
        AppConfig config = new AppConfig(tempDir.resolve("data").resolve("recursive.db"),
                "http://localhost:11434", new com.recursive.application.StoragePathProvider());

        CompositionRoot root = CompositionRoot.build(config);
        try {
            assertThat(root.jobOrchestrator()).isNotNull();
            assertThat(root.modelService()).isNotNull();
            assertThat(root.standardModelService()).isNotNull();
            assertThat(root.openAiCompatibleModelService()).isNotNull();
            assertThat(root.remoteModelDiscovery()).isNotNull();
            assertThat(root.parsingService()).isNotNull();
            assertThat(root.translationOrchestrator()).isNotNull();
            assertThat(root.exportService()).isNotNull();
            assertThat(Files.exists(config.databaseFile())).isTrue();

            Job job = root.jobOrchestrator().createJob("demo", sourcePdf,
                    Language.of("en", "English"), Language.of("de", "Deutsch"), "llama3.1:8b");
            assertThat(root.jobOrchestrator().findJob(job.id())).isPresent();
            root.jobOrchestrator().start(job.id());
            root.jobOrchestrator().complete(job.id());
            assertThat(root.jobOrchestrator().findJob(job.id()).orElseThrow().status())
                    .isEqualTo(JobStatus.COMPLETED);
        } finally {
            root.close();
        }
    }
}
