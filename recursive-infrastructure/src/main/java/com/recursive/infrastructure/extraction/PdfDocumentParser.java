package com.recursive.infrastructure.extraction;

import com.recursive.domain.DocumentPage;
import com.recursive.domain.DocumentParser;
import com.recursive.domain.DocumentStructure;
import com.recursive.domain.ImageRegion;
import com.recursive.domain.ParseException;
import com.recursive.domain.TextSegment;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link DocumentParser} implementation on PDFBox. Encrypted documents are
 * handled with the supplied password; pages with text become segments, and
 * pages whose only content is raster are flagged for the OCR pipeline.
 */
public class PdfDocumentParser implements DocumentParser {

    private final PdfTextExtractor textExtractor;
    private final PdfImageExtractor imageExtractor;

    public PdfDocumentParser(PdfTextExtractor textExtractor, PdfImageExtractor imageExtractor) {
        this.textExtractor = textExtractor;
        this.imageExtractor = imageExtractor;
    }

    public static PdfDocumentParser create() {
        return new PdfDocumentParser(new PdfTextExtractor(), new PdfImageExtractor());
    }

    @Override
    public Optional<DocumentStructure> parse(Path pdfPath, String password) {
        try (PDDocument document = open(pdfPath, password)) {
            List<DocumentPage> pages = new ArrayList<>();
            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
                List<TextSegment> segments = textExtractor.extract(document, pageNumber);
                List<ImageRegion> images = imageExtractor.extract(document, pageNumber);
                pages.add(new DocumentPage(pageNumber, segments, images,
                        segments.isEmpty() && !images.isEmpty()));
            }
            if (pages.stream().allMatch(page -> page.textSegments().isEmpty() && page.imageRegions().isEmpty())) {
                return Optional.empty();
            }
            Path fileName = pdfPath.getFileName();
            return Optional.of(new DocumentStructure(
                    fileName == null ? pdfPath.toString() : fileName.toString(), pages));
        } catch (IOException e) {
            throw new ParseException("Could not read PDF file: " + pdfPath, e);
        }
    }

    private static PDDocument open(Path pdfPath, String password) throws IOException {
        if (password == null || password.isBlank()) {
            return Loader.loadPDF(pdfPath.toFile());
        }
        return Loader.loadPDF(pdfPath.toFile(), password);
    }
}
