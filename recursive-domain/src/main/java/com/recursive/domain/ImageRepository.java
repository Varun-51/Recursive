package com.recursive.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting {@link ImageReference} entities so the reconstructor
 * can re-place images on the translated pages.
 */
public interface ImageRepository {

    ImageReference save(ImageReference image);

    Optional<ImageReference> findById(String id);

    List<ImageReference> findByPageId(String pageId);

    void deleteByPageId(String pageId);
}
