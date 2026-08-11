package com.recursive.application;

import com.recursive.domain.DocumentReconstructor;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class PDFExportServiceTest {

    @Test
    void delegatesToReconstructorInsideJobOutputFolder() {
        StoragePathProvider paths = new StoragePathProvider();
        DocumentReconstructor reconstructor = (outputDirectory, jobId) ->
                outputDirectory.resolve("translated.pdf");
        PDFExportService service = new PDFExportService(reconstructor, paths);

        Path exported = service.export("job-42");

        assertThat(exported).isEqualTo(paths.jobOutput("job-42").resolve("translated.pdf"));
        assertThat(exported.toString()).doesNotContain("..");
    }
}
