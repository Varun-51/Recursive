package com.recursive.application;

/**
 * The disclaimer shown on first launch. Content is deliberately a constant
 * in the application layer so translations of it can override it later
 * without touching the UI.
 */
public class DisclaimerService {

    private static final String DISCLAIMER_TEXT =
            "Recursive runs language models locally on your machine. Generated "
                    + "translations are machine output and may contain errors. Review "
                    + "important documents before use.";

    public String disclaimerText() {
        return DISCLAIMER_TEXT;
    }
}
