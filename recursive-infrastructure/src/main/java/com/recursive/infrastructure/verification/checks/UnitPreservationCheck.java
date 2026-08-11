package com.recursive.infrastructure.verification.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Common units and symbols must be carried over. Letter units are matched
 * as standalone tokens so 'm' inside "month" is not read as the metre
 * unit; symbol units are matched anywhere. The set is deliberately small
 * and Phase 1 English-centric; extending it is a data change, not a code
 * change.
 */
public final class UnitPreservationCheck {

    private static final Set<String> UNITS = Set.of(
            "km", "m", "cm", "mm", "kg", "g", "mg", "l", "ml", "h", "min", "s",
            "mb", "gb", "tb", "hz", "khz", "mhz", "ghz", "°c", "°f", "%", "€", "$", "£");

    private UnitPreservationCheck() {
    }

    public static List<String> run(String original, String translation) {
        List<String> missing = new ArrayList<>();
        for (String unit : UNITS) {
            Pattern pattern = patternFor(unit);
            if (pattern.matcher(original).find() && !pattern.matcher(translation).find()) {
                missing.add("Unit '" + unit + "' is missing from the translation");
            }
        }
        return missing;
    }

    private static Pattern patternFor(String unit) {
        if (unit.matches("[a-z]+")) {
            return Pattern.compile("(?i)(?<![A-Za-z0-9])" + Pattern.quote(unit) + "(?![A-Za-z0-9])");
        }
        return Pattern.compile(Pattern.quote(unit), Pattern.CASE_INSENSITIVE);
    }
}
