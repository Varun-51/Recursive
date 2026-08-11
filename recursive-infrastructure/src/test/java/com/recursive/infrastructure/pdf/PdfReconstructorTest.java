package com.recursive.infrastructure.pdf;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.ImageReference;
import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageStatus;
import com.recursive.domain.Position;
import com.recursive.domain.ValidationStatus;
import com.recursive.infrastructure.database.DatabaseInitializer;
import com.recursive.infrastructure.database.JdbcBlockRepository;
import com.recursive.infrastructure.database.JdbcImageRepository;
import com.recursive.infrastructure.database.JdbcJobRepository;
import com.recursive.infrastructure.database.JdbcPageRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReconstructorTest {

    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    private final Path databaseFile = Path.of("target", "test-db",
            "reconstruct-" + UUID.randomUUID() + ".db");
    private DatabaseInitializer initializer;

    @TempDir
    Path outputDir;

    @AfterEach
    void closeDatabase() {
        if (initializer != null) {
            initializer.close();
        }
    }

    @Test
    void reconstructsPdfWithTranslatedBlocksAndImages() throws IOException {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository jobs = new JdbcJobRepository(initializer.connectionProvider());
        JdbcPageRepository pages = new JdbcPageRepository(initializer.connectionProvider());
        JdbcBlockRepository blocks = new JdbcBlockRepository(initializer.connectionProvider());
        JdbcImageRepository images = new JdbcImageRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        jobs.save(new Job("j1", "nightly", "C:/docs/in.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", null, JobStatus.COMPLETED,
                1, 1, 2, 1, 0, 0, null, now, now));
        pages.save(new Page("p1", "j1", 1, PageStatus.COMPLETED, null, null, null,
                0, null, now, now));
        blocks.save(new Block("b1", "p1", 0, BlockContentType.HEADING, "Welcome",
                new Position(0, 0, 1, 1), new FontInfo("Helvetica", 12f, FontStyle.REGULAR),
                0, "Willkommen", ValidationStatus.PASS, 0.9, 0, null, null, now, now));
        blocks.save(new Block("b2", "p1", 1, BlockContentType.PARAGRAPH, "Hello world",
                new Position(0, 0, 1, 1), null, 1, "Hallo Welt", ValidationStatus.PASS,
                0.95, 0, null, null, now, now));
        blocks.save(new Block("b3", "p1", 2, BlockContentType.PARAGRAPH, "Not yet done",
                new Position(0, 0, 1, 1), null, 2, null, ValidationStatus.NEEDS_REVIEW,
                null, 0, null, null, now, now));
        images.save(new ImageReference("i1", "p1", 0, new Position(0, 0, 0, 0),
                ONE_PIXEL_PNG, "png", now));

        PdfReconstructor reconstructor =
                new PdfReconstructor(pages, blocks, images);
        Path pdf = reconstructor.reconstruct(outputDir, "j1");

        assertThat(Files.exists(pdf)).isTrue();
        assertThat(Files.size(pdf)).isPositive();
        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }
}
