package com.recursive.domain;

import java.util.List;

/**
 * Local context a translator and validator need to produce consistent output.
 * Blocks are processed one at a time; carrying whole blocks would defeat the
 * bounded-memory design, so only the neighboring texts and the active section
 * heading travel with a block.
 *
 * @param previousBlockText text of the previous block, if any
 * @param nextBlockText     text of the next block, if any
 * @param sectionHeading    heading of the section the block belongs to
 * @param glossaryTerms     locked glossary terms for this job
 */
public record ProcessingContext(
        String previousBlockText,
        String nextBlockText,
        String sectionHeading,
        List<GlossaryTerm> glossaryTerms) {

    public ProcessingContext {
        if (glossaryTerms == null) {
            throw new IllegalArgumentException("glossaryTerms must not be null; pass an empty list");
        }
        glossaryTerms = List.copyOf(glossaryTerms);
    }

    public static ProcessingContext empty() {
        return new ProcessingContext(null, null, null, List.of());
    }
}
