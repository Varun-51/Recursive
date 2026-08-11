package com.recursive.infrastructure.extraction;

import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.Position;
import com.recursive.domain.TextSegment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.util.List;

/**
 * PDFBox-backed text extraction for one page. Positions are not available
 * from the default stripper, so segments carry a zeroed placement and the
 * default font; the reconstructor flows text without absolute layout in
 * Phase 1.
 */
public class PdfTextExtractor {

    private static final FontInfo UNKNOWN_FONT = new FontInfo("Unknown", 12f, FontStyle.REGULAR);
    private static final Position UNKNOWN_POSITION = new Position(0, 0, 0, 0);

    public List<TextSegment> extract(PDDocument document, int pageNumber) {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        try {
            String text = stripper.getText(document);
            return text.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> new TextSegment(line, UNKNOWN_POSITION, UNKNOWN_FONT))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Could not extract text from page " + pageNumber, e);
        }
    }
}
