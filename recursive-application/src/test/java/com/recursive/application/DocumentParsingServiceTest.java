package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.DocumentPage;
import com.recursive.domain.DocumentParser;
import com.recursive.domain.DocumentStructure;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.ImageReference;
import com.recursive.domain.ImageRegion;
import com.recursive.domain.ImageRepository;
import com.recursive.domain.OcrEngine;
import com.recursive.domain.OcrResult;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.Position;
import com.recursive.domain.TextSegment;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParsingServiceTest {

    @Test
    void persistsBlocksImagesAndPagesFromParse() {
        InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        InMemoryImageRepository images = new InMemoryImageRepository();
        InMemoryPageRepository pages = new InMemoryPageRepository();
        DocumentParsingService service = new DocumentParsingService(parserWithOnePage(false), neverOcr(),
                pages, blocks, images);

        int count = service.ingest("job-1", Path.of("C:/docs/in.pdf"), null);

        assertThat(count).isEqualTo(1);
        assertThat(pages.byJob("job-1")).hasSize(1);
        assertThat(blocks.all()).hasSize(2);
        assertThat(images.all()).hasSize(1);
    }

    @Test
    void classifiesShortTitleLinesAsHeadings() {
        InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        DocumentParsingService service = new DocumentParsingService(
                new DocumentParser() {
                    @Override
                    public Optional<DocumentStructure> parse(Path pdfPath, String password) {
                        return Optional.of(new DocumentStructure("in.pdf", List.of(
                                new DocumentPage(1,
                                        List.of(new TextSegment("Quarterly Report", new Position(0, 0, 10, 10),
                                                        new FontInfo("Arial", 12f, FontStyle.BOLD)),
                                                new TextSegment("Revenue grew 12%.", new Position(0, 0, 10, 10),
                                                        new FontInfo("Arial", 12f, FontStyle.REGULAR))),
                                        List.of(), false))));
                    }
                }, neverOcr(), new InMemoryPageRepository(), blocks, new InMemoryImageRepository());

        service.ingest("job-1", Path.of("C:/docs/in.pdf"), null);

        assertThat(blocks.all()).extracting(Block::contentType)
                .containsExactly(BlockContentType.HEADING, BlockContentType.PARAGRAPH);
    }

    @Test
    void routesScannedPagesThroughOcr() {
        InMemoryBlockRepository blocks = new InMemoryBlockRepository();
        DocumentParsingService service = new DocumentParsingService(
                parserWithOnePage(true), ocrReturningSegment(), new InMemoryPageRepository(),
                blocks, new InMemoryImageRepository());

        service.ingest("job-1", Path.of("C:/docs/in.pdf"), null);

        assertThat(blocks.all()).hasSize(1);
        assertThat(blocks.all().get(0).originalText()).isEqualTo("Recognized text");
    }

    @Test
    void skipsPagesWithNoContent() {
        InMemoryPageRepository pages = new InMemoryPageRepository();
        DocumentParsingService service = new DocumentParsingService(
                new DocumentParser() {
                    @Override
                    public Optional<DocumentStructure> parse(Path pdfPath, String password) {
                        return Optional.of(new DocumentStructure("blank.pdf",
                                List.of(new DocumentPage(1, List.of(), List.of(), false))));
                    }
                }, neverOcr(), pages, new InMemoryBlockRepository(), new InMemoryImageRepository());

        service.ingest("job-1", Path.of("C:/docs/in.pdf"), null);

        assertThat(pages.byJob("job-1")).hasSize(1);
    }

    private static DocumentParser parserWithOnePage(boolean requiresOcr) {
        return new DocumentParser() {
            @Override
            public Optional<DocumentStructure> parse(Path pdfPath, String password) {
                List<TextSegment> segments = requiresOcr ? List.of()
                        : List.of(new TextSegment("Hello", new Position(0, 0, 10, 10),
                                        new FontInfo("Arial", 12f, FontStyle.REGULAR)),
                                new TextSegment("World", new Position(0, 0, 10, 10),
                                        new FontInfo("Arial", 12f, FontStyle.REGULAR)));
                List<ImageRegion> regions = List.of(new ImageRegion(0, new Position(1, 1, 2, 2),
                        new byte[]{1, 2, 3}, "png"));
                return Optional.of(new DocumentStructure("in.pdf",
                        List.of(new DocumentPage(1, segments, regions, requiresOcr))));
            }
        };
    }

    private static OcrEngine neverOcr() {
        return (pageImage, imageFormat) -> Optional.empty();
    }

    private static OcrEngine ocrReturningSegment() {
        return (pageImage, imageFormat) -> Optional.of(new OcrResult(
                List.of(new TextSegment("Recognized text", new Position(0, 0, 10, 10),
                        new FontInfo("OCR", 10f, FontStyle.REGULAR))),
                com.recursive.domain.Confidence.of(0.9)));
    }

    private static class InMemoryBlockRepository implements BlockRepository {
        private final List<Block> all = new ArrayList<>();

        @Override
        public Block save(Block block) {
            all.add(block);
            return block;
        }

        @Override
        public Optional<Block> findById(String id) {
            return all.stream().filter(b -> b.id().equals(id)).findFirst();
        }

        @Override
        public List<Block> findByPageId(String pageId) {
            return all.stream().filter(b -> b.pageId().equals(pageId)).toList();
        }

        @Override
        public List<Block> findUnprocessedByPageId(String pageId) {
            return findByPageId(pageId).stream()
                    .filter(b -> b.translatedText() == null).toList();
        }

        @Override
        public void deleteByPageId(String pageId) {
            all.removeIf(b -> b.pageId().equals(pageId));
        }

        List<Block> all() {
            return all;
        }
    }

    private static class InMemoryImageRepository implements ImageRepository {
        private final List<ImageReference> all = new ArrayList<>();

        @Override
        public ImageReference save(ImageReference image) {
            all.add(image);
            return image;
        }

        @Override
        public Optional<ImageReference> findById(String id) {
            return all.stream().filter(i -> i.id().equals(id)).findFirst();
        }

        @Override
        public List<ImageReference> findByPageId(String pageId) {
            return all.stream().filter(i -> i.pageId().equals(pageId)).toList();
        }

        @Override
        public void deleteByPageId(String pageId) {
            all.removeIf(i -> i.pageId().equals(pageId));
        }

        List<ImageReference> all() {
            return all;
        }
    }

    private static class InMemoryPageRepository implements PageRepository {
        private final List<Page> all = new ArrayList<>();

        @Override
        public Page save(Page page) {
            all.add(page);
            return page;
        }

        @Override
        public Optional<Page> findById(String id) {
            return all.stream().filter(p -> p.id().equals(id)).findFirst();
        }

        @Override
        public List<Page> findByJobId(String jobId) {
            return byJob(jobId);
        }

        @Override
        public List<Page> findIncompleteByJobId(String jobId) {
            return byJob(jobId).stream().filter(p -> p.status() != com.recursive.domain.PageStatus.COMPLETED).toList();
        }

        @Override
        public void deleteByJobId(String jobId) {
            all.removeIf(p -> p.jobId().equals(jobId));
        }

        List<Page> byJob(String jobId) {
            return all.stream().filter(p -> p.jobId().equals(jobId)).toList();
        }
    }
}
