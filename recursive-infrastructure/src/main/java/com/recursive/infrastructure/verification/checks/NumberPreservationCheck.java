package com.recursive.infrastructure.verification.checks;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Every number in the original must survive the translation. Numbers are
 * matched as signed decimals with comma or dot separators; a missing number
 * is reported verbatim so the re-translation prompt can fix it.
 */
public final class NumberPreservationCheck {

    private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:[.,]\\d+)?");

    private NumberPreservationCheck() {
    }

    public static List<String> run(String original, String translation) {
        List<String> numbers = NUMBER.matcher(original).results()
                .map(MatchResult::group).distinct().toList();
        return numbers.stream()
                .filter(number -> !translation.contains(number))
                .map(number -> "Number '" + number + "' is missing from the translation")
                .toList();
    }
}
