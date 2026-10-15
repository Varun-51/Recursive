package com.recursive.application;

import com.recursive.domain.LicenseRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseServiceTest {

    @Test
    void startsUnacceptedAndStaysAcceptedOnceConfirmed() {
        LicenseService service = new LicenseService(new LicenseRepository() {
            private boolean accepted = false;
            public boolean isAccepted() { return accepted; }
            public void accept() { accepted = true; }
        });
        assertThat(service.isAccepted()).isFalse();

        service.accept();

        assertThat(service.isAccepted()).isTrue();
    }
}
