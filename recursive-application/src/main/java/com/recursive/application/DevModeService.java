package com.recursive.application;

/**
 * Development-mode configuration: feature toggles that are meaningless in
 * production (screenshot capture, designer theme) plus the master switch.
 * Kept as plain state; persistence belongs to the infrastructure layer.
 */
public class DevModeService {

    private boolean enabled;
    private boolean screenshotsEnabled;
    private boolean customThemeEnabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean screenshotsEnabled() {
        return enabled && screenshotsEnabled;
    }

    public boolean customThemeEnabled() {
        return enabled && customThemeEnabled;
    }

    public void setScreenshotsEnabled(boolean screenshotsEnabled) {
        this.screenshotsEnabled = screenshotsEnabled;
    }

    public void setCustomThemeEnabled(boolean customThemeEnabled) {
        this.customThemeEnabled = customThemeEnabled;
    }
}
