package com.recursive.application;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.DocumentPage;
import com.recursive.domain.DocumentParser;
import com.recursive.domain.DocumentStructure;
import com.recursive.domain.ImageReference;
import com.recursive.domain.ImageRegion;
import com.recursive.domain.ImageRepository;
import com.recursive.domain.OcrEngine;
import com.recursive.domain.OcrResult;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import com.recursive.domain.PageStatus;
import com.recursive.domain.TextSegment;
import com.recursive.domain.ValidationStatus;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Ingests a parsed document into the persistence layer. Text segments
 * become blocks; embedded rasters become image references; scanned pages
 * are routed through the OCR engine and their recognized text becomes
 * blocks with the page's layout.
 */
public class DocumentParsingService {

    private final DocumentParser parser;
    private final OcrEngine ocrEngine;
    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final ImageRepository imageRepository;

    public DocumentParsingService(DocumentParser parser, OcrEngine ocrEngine,
                                  PageRepository pageRepository, BlockRepository blockRepository,
                                  ImageRepository imageRepository) {
        this.parser = parser;
        this.ocrEngine = ocrEngine;
        this.pageRepository = pageRepository;
        this.blockRepository = blockRepository;
        this.imageRepository = imageRepository;
    }

    /**
     * @return number of pages persisted for the job
     */
    public int ingest(String jobId, Path pdfPath, String password) {
        Optional<DocumentStructure> structure = parser.parse(pdfPath, password);
        if (structure.isEmpty()) {
            return 0;
        }
        for (DocumentPage page : structure.get().pages()) {
            persistPage(jobId, page);
        }
        return structure.get().pages().size();
    }

    private void persistPage(String jobId, DocumentPage page) {
        String pageId = UUID.randomUUID().toString();
        List<TextSegment> segments = segmentsFor(page);
        if (segments.isEmpty()) {
            Page skipped = new Page(pageId, jobId, page.pageNumber(), PageStatus.COMPLETED,
                    null, null, null, 0, null, Instant.now(), Instant.now());
            pageRepository.save(skipped);
            return;
        }
        Page persisted = new Page(pageId, jobId, page.pageNumber(), PageStatus.COMPLETED,
                null, null, null, 0, null, Instant.now(), Instant.now());
        pageRepository.save(persisted);
        persistImages(pageId, page.imageRegions());
        persistBlocks(pageId, segments);
    }

    private List<TextSegment> segmentsFor(DocumentPage page) {
        if (!page.textSegments().isEmpty()) {
            return page.textSegments();
        }
        if (!page.requiresOcr() || page.imageRegions().isEmpty()) {
            return List.of();
        }
        ImageRegion source = page.imageRegions().get(0);
        Optional<OcrResult> recognized = ocrEngine.recognize(source.imageData(), source.originalFormat());
        return recognized.map(OcrResult::segments).orElse(List.of());
    }

    private void persistImages(String pageId, List<ImageRegion> regions) {
        for (ImageRegion region : regions) {
            ImageReference reference = new ImageReference(UUID.randomUUID().toString(), pageId,
                    region.imageIndex(), region.position(), region.imageData(), region.originalFormat(),
                    Instant.now());
            imageRepository.save(reference);
        }
    }

    private void persistBlocks(String pageId, List<TextSegment> segments) {
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            Block block = new Block(UUID.randomUUID().toString(), pageId, i,
                    classify(segment.text()), segment.text(), segment.position(), segment.fontInfo(),
                    i, null, ValidationStatus.PENDING, null, 0, null, null,
                    Instant.now(), Instant.now());
            blockRepository.save(block);
        }
    }

    private static BlockContentType classify(String text) {
        if (text.length() <= 60 && !text.endsWith(".") && Character.isUpperCase(text.charAt(0))
                && text.chars().noneMatch(Character::isDigit)) {
            return BlockContentType.HEADING;
        }
        return BlockContentType.PARAGRAPH;
    }
}
