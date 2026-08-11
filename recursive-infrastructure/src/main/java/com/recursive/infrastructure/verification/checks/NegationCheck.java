package com.recursive.infrastructure.verification.checks;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Dropping a negation inverts the meaning; a translation of a negated
 * source that contains no negation token is flagged. The token list is
 * Phase 1 English; other source languages extend the set.
 */
public final class NegationCheck {

    private static final Set<String> NEGATIONS = Set.of(
            "not", "no", "never", "don't", "dont", "doesn't", "doesnt",
            "can't", "cant", "cannot", "without", "none", "nothing");

    private NegationCheck() {
    }

    public static List<String> run(String original, String translation) {
        String lowerOriginal = original.toLowerCase(Locale.ROOT);
        String lowerTranslation = translation.toLowerCase(Locale.ROOT);
        if (NEGATIONS.stream().anyMatch(lowerOriginal::contains)
                && NEGATIONS.stream().noneMatch(lowerTranslation::contains)) {
            return List.of("The translation drops a negation; meaning may be inverted");
        }
        return List.of();
    }
}
