package com.recursive.infrastructure.extraction;

import com.recursive.domain.ImageRegion;
import com.recursive.domain.Position;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PDFBox-backed image extraction for one page. Rasters embedded in the
 * page are read as encoded bytes so ingestion can persist them without
 * re-opening the PDF. Placement is unknown in Phase 1 (zeroed position).
 */
public class PdfImageExtractor {

    public List<ImageRegion> extract(PDDocument document, int pageNumber) {
        List<ImageRegion> regions = new ArrayList<>();
        PDResources resources = document.getPage(pageNumber - 1).getResources();
        if (resources == null) {
            return regions;
        }
        try {
            int index = 0;
            for (COSName name : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(name);
                if (xObject instanceof PDImageXObject image) {
                    regions.add(toRegion(image, index++));
                }
            }
            return regions;
        } catch (IOException e) {
            throw new IllegalStateException("Could not read images from page " + pageNumber, e);
        }
    }

    private static ImageRegion toRegion(PDImageXObject image, int index) {
        try {
            byte[] encoded;
            try (InputStream stream = image.createInputStream()) {
                encoded = stream.readAllBytes();
            }
            return new ImageRegion(index, new Position(0, 0, 0, 0),
                    encoded, image.getSuffix());
        } catch (IOException e) {
            throw new IllegalStateException("Could not read embedded image " + index, e);
        }
    }
}
