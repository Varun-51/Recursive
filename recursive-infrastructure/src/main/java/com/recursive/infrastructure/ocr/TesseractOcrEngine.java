package com.recursive.infrastructure.ocr;

import com.recursive.domain.Confidence;
import com.recursive.domain.FontInfo;
import com.recursive.domain.FontStyle;
import com.recursive.domain.OcrEngine;
import com.recursive.domain.OcrResult;
import com.recursive.domain.Position;
import com.recursive.domain.TextSegment;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * {@link OcrEngine} implementation on Tess4J. Recognition is per page
 * image; recognized lines become segments with unknown placement. Tess4J
 * exposes no per-page confidence, so a neutral placeholder is reported and
 * the validator treats OCR text as needing verification.
 */
public class TesseractOcrEngine implements OcrEngine {

    private static final FontInfo DEFAULT_FONT = new FontInfo("OCR", 12f, FontStyle.REGULAR);
    private static final Position UNKNOWN_POSITION = new Position(0, 0, 0, 0);
    private static final Confidence NEUTRAL_CONFIDENCE = Confidence.of(0.5);

    private final ITesseract tesseract;

    public TesseractOcrEngine() {
        this.tesseract = new Tesseract();
    }

    public TesseractOcrEngine(ITesseract tesseract) {
        this.tesseract = tesseract;
    }

    @Override
    public Optional<OcrResult> recognize(byte[] pageImage, String imageFormat) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(pageImage));
            if (image == null) {
                throw new IOException("Unsupported image format: " + imageFormat);
            }
            String text = tesseract.doOCR(image);
            List<TextSegment> segments = text.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(line -> new TextSegment(line, UNKNOWN_POSITION, DEFAULT_FONT))
                    .toList();
            return segments.isEmpty() ? Optional.empty() : Optional.of(new OcrResult(segments, NEUTRAL_CONFIDENCE));
        } catch (IOException | TesseractException e) {
            return Optional.empty();
        }
    }
}
