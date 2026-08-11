package com.recursive.infrastructure.verification.checks;

import com.recursive.domain.ProcessingContext;

import java.util.List;

/**
 * Locked glossary entries are binding: the translation must contain the
 * locked target term. Suggestions (unlocked entries) are not enforced.
 */
public final class TerminologyCheck {

    private TerminologyCheck() {
    }

    public static List<String> run(String translation, ProcessingContext context) {
        return context.glossaryTerms().stream()
                .filter(term -> term.locked())
                .filter(term -> !translation.contains(term.targetTerm()))
                .map(term -> "Locked term '" + term.sourceTerm() + "' must be translated as '"
                        + term.targetTerm() + "'")
                .toList();
    }
}
