package com.recursive.infrastructure.database;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.GlossaryTerm;
import com.recursive.domain.ImageReference;
import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.domain.Page;
import com.recursive.domain.PageStatus;
import com.recursive.domain.Position;
import com.recursive.domain.ValidationStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcRepositoriesTest {

    private final Path databaseFile = Path.of("target", "test-db",
            "repos-" + UUID.randomUUID() + ".db");
    private DatabaseInitializer initializer;

    @AfterEach
    void closeDatabase() {
        if (initializer != null) {
            initializer.close();
        }
    }

    @Test
    void pageBlockImageGlossaryRoundTrip() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository jobs = new JdbcJobRepository(initializer.connectionProvider());
        JdbcPageRepository pages = new JdbcPageRepository(initializer.connectionProvider());
        JdbcBlockRepository blocks = new JdbcBlockRepository(initializer.connectionProvider());
        JdbcImageRepository images = new JdbcImageRepository(initializer.connectionProvider());
        JdbcGlossaryRepository glossary = new JdbcGlossaryRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        jobs.save(new Job("j1", "nightly", "C:/docs/in.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", null, JobStatus.PROCESSING,
                0, 0, 0, 0, 0, 0, null, now, now));

        Page page = new Page("p1", "j1", 1, PageStatus.COMPLETED, null, null, null,
                0, null, now, now);
        pages.save(page);

        Block block = new Block("b1", "p1", 0, BlockContentType.PARAGRAPH, "Hello",
                new Position(10, 20, 300, 12), new FontInfo("Helvetica", 12f, FontStyle.REGULAR),
                0, "Hallo", ValidationStatus.PASS, 0.875, 2, null, null, now, now);
        blocks.save(block);

        ImageReference image = new ImageReference("i1", "p1", 0, new Position(1, 2, 3, 4),
                new byte[]{1, 2, 3}, "png", now);
        images.save(image);

        GlossaryTerm term = new GlossaryTerm("g1", "j1", "invoice", "Rechnung", "finance",
                true, 7, now);
        glossary.save(term);

        Page loadedPage = pages.findById("p1").orElseThrow();
        assertThat(loadedPage.status()).isEqualTo(PageStatus.COMPLETED);
        assertThat(pages.findIncompleteByJobId("j1")).isEmpty();

        Block loadedBlock = blocks.findById("b1").orElseThrow();
        assertThat(loadedBlock.translatedText()).isEqualTo("Hallo");
        assertThat(loadedBlock.validationStatus()).isEqualTo(ValidationStatus.PASS);
        assertThat(loadedBlock.confidenceScore()).isEqualTo(0.875);
        assertThat(loadedBlock.retryCount()).isEqualTo(2);
        assertThat(loadedBlock.fontInfo().name()).isEqualTo("Helvetica");
        assertThat(loadedBlock.position().width()).isEqualTo(300f);
        assertThat(blocks.findUnprocessedByPageId("p1")).isEmpty();

        ImageReference loadedImage = images.findByPageId("p1").get(0);
        assertThat(loadedImage.imageData()).containsExactly(1, 2, 3);

        assertThat(glossary.findLockedByJobId("j1")).extracting(GlossaryTerm::targetTerm)
                .containsExactly("Rechnung");
        assertThat(glossary.findByJobId("j1").get(0).occurrences()).isEqualTo(7);

        pages.deleteByJobId("j1");
        assertThat(pages.findByJobId("j1")).isEmpty();
        assertThat(blocks.findByPageId("p1")).isEmpty();
        assertThat(images.findByPageId("p1")).isEmpty();
        assertThat(glossary.findByJobId("j1")).hasSize(1);

        glossary.deleteByJobId("j1");
        assertThat(glossary.findByJobId("j1")).isEmpty();
    }

    @Test
    void nullConfidenceRoundTripsAsNull() {
        initializer = new DatabaseInitializer(databaseFile);
        initializer.initialize();
        JdbcJobRepository jobs = new JdbcJobRepository(initializer.connectionProvider());
        JdbcPageRepository pages = new JdbcPageRepository(initializer.connectionProvider());
        JdbcBlockRepository blocks = new JdbcBlockRepository(initializer.connectionProvider());
        Instant now = Instant.now();
        jobs.save(new Job("j1", "nightly", "C:/docs/in.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", null, JobStatus.PROCESSING,
                0, 0, 0, 0, 0, 0, null, now, now));
        pages.save(new Page("p1", "j1", 1, PageStatus.PENDING, null, null, null,
                0, null, now, now));
        Block block = new Block("b1", "p1", 0, BlockContentType.PARAGRAPH, "Hello",
                new Position(0, 0, 1, 1), null, 0, null, ValidationStatus.PENDING, null,
                0, null, null, now, now);

        blocks.save(block);

        Block loaded = blocks.findById("b1").orElseThrow();
        assertThat(loaded.confidenceScore()).isNull();
        assertThat(loaded.fontInfo()).isNull();
    }
}
