package com.recursive.infrastructure.pdf;

import com.recursive.domain.Block;
import com.recursive.domain.BlockContentType;
import com.recursive.domain.BlockRepository;
import com.recursive.domain.DocumentReconstructor;
import com.recursive.domain.ImageReference;
import com.recursive.domain.ImageRepository;
import com.recursive.domain.Page;
import com.recursive.domain.PageRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * {@link DocumentReconstructor} implementation on PDFBox. Phase 1 layout:
 * blocks flow top-down as single lines with heading emphasis, images are
 * re-embedded at their recorded spot. Text expansion and overflow handling
 * are Phase 2 layout engineering.
 */
public class PdfReconstructor implements DocumentReconstructor {

    private static final Logger log = LoggerFactory.getLogger(PdfReconstructor.class);

    private final PageRepository pageRepository;
    private final BlockRepository blockRepository;
    private final ImageRepository imageRepository;

    public PdfReconstructor(PageRepository pageRepository, BlockRepository blockRepository,
                            ImageRepository imageRepository) {
        this.pageRepository = pageRepository;
        this.blockRepository = blockRepository;
        this.imageRepository = imageRepository;
    }

    @Override
    public Path reconstruct(Path outputDirectory, String jobId) {
        List<Page> pages = pageRepository.findByJobId(jobId).stream()
                .sorted(Comparator.comparingInt(Page::pageNumber)).toList();
        try (PDDocument document = new PDDocument()) {
            for (Page page : pages) {
                renderPage(document, page);
            }
            Path output = outputDirectory.resolve(jobId + ".pdf");
            document.save(output.toFile());
            log.info("Reconstructed translated PDF {} with {} pages", output, pages.size());
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Could not reconstruct PDF for job " + jobId, e);
        }
    }

    private void renderPage(PDDocument document, Page page) throws IOException {
        PDPage pdfPage = new PDPage(PDRectangle.A4);
        document.addPage(pdfPage);
        List<Block> blocks = blockRepository.findByPageId(page.id()).stream()
                .sorted(Comparator.comparingInt(Block::readingOrder)).toList();
        List<ImageReference> images = imageRepository.findByPageId(page.id());
        try (PDPageContentStream content = new PDPageContentStream(document, pdfPage)) {
            content.beginText();
            content.newLineAtOffset(50, 780);
            float lineHeight = 16;
            for (Block block : blocks) {
                content.setFont(fontFor(block), 12);
                content.showText(textOf(block));
                content.newLineAtOffset(0, -lineHeight);
            }
            content.endText();
            for (ImageReference image : images) {
                drawImage(document, content, image);
            }
        }
    }

    private static PDType1Font fontFor(Block block) {
        return block.contentType() == BlockContentType.HEADING
                || block.contentType() == BlockContentType.SUBHEADING
                ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    }

    private static String textOf(Block block) {
        String text = block.translatedText() != null ? block.translatedText() : block.originalText();
        return text.replace('\n', ' ');
    }

    private static void drawImage(PDDocument document, PDPageContentStream content,
                                  ImageReference image) throws IOException {
        try {
            PDImageXObject pdfImage = PDImageXObject.createFromByteArray(
                    document, image.imageData(), image.originalFormat());
            content.drawImage(pdfImage, image.position().x(), image.position().y(),
                    pdfImage.getWidth(), pdfImage.getHeight());
        } catch (IOException e) {
            log.warn("Skipping unreadable image on page {}", image.pageId());
        }
    }
}
