package com.jobflow.orchestrator.scheduler;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BackoffCalculatorTest {

    private final BackoffCalculator calculator = new BackoffCalculator();

    @Test
    void firstAttemptUsesBaseDelay() {
        assertThat(calculator.delayMillis(1000, 60_000, 1)).isEqualTo(1000);
    }

    @Test
    void delayDoublesEachAttempt() {
        assertThat(calculator.delayMillis(1000, 1_000_000, 1)).isEqualTo(1000);
        assertThat(calculator.delayMillis(1000, 1_000_000, 2)).isEqualTo(2000);
        assertThat(calculator.delayMillis(1000, 1_000_000, 3)).isEqualTo(4000);
        assertThat(calculator.delayMillis(1000, 1_000_000, 4)).isEqualTo(8000);
    }

    @Test
    void delayNeverExceedsCeiling() {
        long delay = calculator.delayMillis(1000, 5000, 10);
        assertThat(delay).isEqualTo(5000);
    }

    @Test
    void zeroOrNegativeAttemptFallsBackToBase() {
        assertThat(calculator.delayMillis(1000, 60_000, 0)).isEqualTo(1000);
    }
}
