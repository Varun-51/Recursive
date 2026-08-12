package com.recursive.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseServiceTest {

    @Test
    void startsUnacceptedAndStaysAcceptedOnceConfirmed() {
        LicenseService service = new LicenseService();
        assertThat(service.isAccepted()).isFalse();

        service.accept();

        assertThat(service.isAccepted()).isTrue();
    }
}
