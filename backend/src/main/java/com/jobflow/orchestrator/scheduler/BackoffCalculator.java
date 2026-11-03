package com.jobflow.orchestrator.scheduler;

import org.springframework.stereotype.Component;

/**
 * Exponential backoff with a configurable ceiling: delay = base * 2^(attempt-1),
 * capped at maxBackoffMs.
 */
@Component
public class BackoffCalculator {

    public long delayMillis(long baseBackoffMs, long maxBackoffMs, int attemptNumber) {
        if (attemptNumber < 1) {
            return baseBackoffMs;
        }
        double raw = baseBackoffMs * Math.pow(2, attemptNumber - 1);
        if (raw <= 0 || raw > maxBackoffMs) {
            return maxBackoffMs;
        }
        return (long) raw;
    }
}
