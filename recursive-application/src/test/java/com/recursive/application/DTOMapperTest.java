package com.recursive.application;

import com.recursive.application.DTOMapper.BlockView;
import com.recursive.application.DTOMapper.JobView;
import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.Job;
import com.recursive.domain.JobStatus;
import com.recursive.domain.Language;
import com.recursive.domain.Position;
import com.recursive.domain.ValidationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DTOMapperTest {

    @Test
    void mapsJobToViewWithCounters() {
        Job job = new Job("j1", "nightly", "C:/docs/in.pdf", Language.of("en", "English"),
                Language.of("de", "Deutsch"), "llama3.1:8b", null, JobStatus.PROCESSING,
                10, 4, 100, 40, 30, 5, null, Instant.now(), Instant.now());

        JobView view = new DTOMapper().toJobView(job);

        assertThat(view.id()).isEqualTo("j1");
        assertThat(view.status()).isEqualTo(JobStatus.PROCESSING);
        assertThat(view.completedBlocks()).isEqualTo(40);
    }

    @Test
    void mapsBlockConfidenceToPercent() {
        Block block = new Block("b1", "p1", 0, BlockContentType.PARAGRAPH, "Hello",
                new Position(0, 0, 1, 1), new FontInfo("Arial", 12f, FontStyle.REGULAR),
                0, "Hallo", ValidationStatus.PASS, 0.875, 1, null, null,
                Instant.now(), Instant.now());

        BlockView view = new DTOMapper().toBlockView(block);

        assertThat(view.confidencePercent()).isEqualTo(87.5);
        assertThat(view.translatedText()).isEqualTo("Hallo");
    }

    @Test
    void nullConfidenceStaysNull() {
        Block block = new Block("b1", "p1", 0, BlockContentType.PARAGRAPH, "Hello",
                new Position(0, 0, 1, 1), new FontInfo("Arial", 12f, FontStyle.REGULAR),
                0, null, ValidationStatus.PENDING, null, 0, null, null,
                Instant.now(), Instant.now());

        BlockView view = new DTOMapper().toBlockView(block);

        assertThat(view.confidencePercent()).isNull();
    }

    @Test
    void mapsListsAndNulls() {
        DTOMapper mapper = new DTOMapper();
        assertThat(mapper.toJobViews(List.of())).isEmpty();
        assertThat(mapper.toJobView(null)).isNull();
        assertThat(mapper.toBlockViews(List.of())).isEmpty();
    }
}
