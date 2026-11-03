package com.jobflow.orchestrator.messaging;

import java.time.Instant;
import java.util.UUID;

/** Lifecycle event published to Kafka on job completion, retry, and failure. */
public record JobLifecycleEvent(
        UUID jobId,
        UUID executionId,
        String eventType, // COMPLETED, RETRY, FAILED
        int attemptNumber,
        String message,
        Instant occurredAt
) {
}
