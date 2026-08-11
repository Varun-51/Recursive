package com.recursive.application;

import com.recursive.domain.DocumentReconstructor;

import java.nio.file.Path;

/**
 * Produces the translated PDF for a finished job. Layout engineering is
 * delegated to the reconstructor port; this service owns only the output
 * location policy.
 */
public class PDFExportService {

    private final DocumentReconstructor reconstructor;
    private final StoragePathProvider storagePaths;

    public PDFExportService(DocumentReconstructor reconstructor, StoragePathProvider storagePaths) {
        this.reconstructor = reconstructor;
        this.storagePaths = storagePaths;
    }

    public Path export(String jobId) {
        return reconstructor.reconstruct(storagePaths.jobOutput(jobId), jobId);
    }
}
