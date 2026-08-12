package com.recursive.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DisclaimerServiceTest {

    @Test
    void disclaimerIsPresentAndNonEmpty() {
        String text = new DisclaimerService().disclaimerText();
        assertThat(text).isNotBlank();
        assertThat(text).contains("Recursive");
    }
}
