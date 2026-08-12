package com.recursive.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DevModeServiceTest {

    @Test
    void togglesAreInertWhileDisabled() {
        DevModeService service = new DevModeService();
        service.setScreenshotsEnabled(true);
        service.setCustomThemeEnabled(true);

        assertThat(service.screenshotsEnabled()).isFalse();
        assertThat(service.customThemeEnabled()).isFalse();
    }

    @Test
    void masterSwitchGatesFeatureToggles() {
        DevModeService service = new DevModeService();
        service.setEnabled(true);
        service.setScreenshotsEnabled(true);

        assertThat(service.screenshotsEnabled()).isTrue();
        assertThat(service.customThemeEnabled()).isFalse();
    }
}
