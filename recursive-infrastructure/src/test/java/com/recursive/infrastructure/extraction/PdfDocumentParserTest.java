package com.recursive.infrastructure.extraction;

import com.recursive.domain.DocumentStructure;
import com.recursive.domain.ParseException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfDocumentParserTest {

    @TempDir
    Path tempDir;

    private final PdfDocumentParser parser = PdfDocumentParser.create();

    @Test
    void parsesTextPageIntoSegments() throws IOException {
        Path pdf = writePdf();

        Optional<DocumentStructure> parsed = parser.parse(pdf, null);

        assertThat(parsed).isPresent();
        assertThat(parsed.get().fileName()).isEqualTo(pdf.getFileName().toString());
        assertThat(parsed.get().pages()).hasSize(1);
        assertThat(parsed.get().pages().get(0).textSegments())
                .extracting(segment -> segment.text())
                .anyMatch(line -> line.contains("Hello Recursive"));
        assertThat(parsed.get().pages().get(0).requiresOcr()).isFalse();
    }

    @Test
    void returnsEmptyForBlankDocument() throws IOException {
        Path pdf = tempDir.resolve("blank.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }

        Optional<DocumentStructure> parsed = parser.parse(pdf, null);

        assertThat(parsed).isEmpty();
    }

    @Test
    void handlesMissingFileGracefully() {
        Path missing = tempDir.resolve("missing.pdf");

        assertThatThrownBy(() -> parser.parse(missing, null))
                .isInstanceOf(ParseException.class);
    }

    private Path writePdf() throws IOException {
        Path pdf = tempDir.resolve("sample.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Hello Recursive");
                content.endText();
            }
            document.save(pdf.toFile());
        }
        assertThat(Files.exists(pdf)).isTrue();
        return pdf;
    }
}
