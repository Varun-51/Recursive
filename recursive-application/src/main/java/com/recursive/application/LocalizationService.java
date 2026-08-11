package com.recursive.application;

import java.util.Locale;

/**
 * Localization facade for Phase 1: the display name of the application and
 * a key lookup that degrades to the key itself. Full resource bundles are
 * an infrastructure concern; this keeps the app layer free of I/O.
 */
public class LocalizationService {

    private final String appName;
    private final Locale locale;

    public LocalizationService(String appName, Locale locale) {
        if (appName == null || appName.isBlank()) {
            throw new IllegalArgumentException("appName must not be blank");
        }
        this.appName = appName;
        this.locale = locale;
    }

    public static LocalizationService defaults() {
        return new LocalizationService("Recursive", Locale.getDefault());
    }

    public String appName() {
        return appName;
    }

    public Locale locale() {
        return locale;
    }

    public String text(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return key;
    }
}
