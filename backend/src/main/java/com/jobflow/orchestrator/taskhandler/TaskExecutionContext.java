package com.jobflow.orchestrator.taskhandler;

import java.util.UUID;

/**
 * Immutable snapshot of everything a TaskHandler needs to run one attempt.
 */
public record TaskExecutionContext(
        UUID jobId,
        UUID executionId,
        int attemptNumber,
        int maxAttempts,
        String payloadJson
) {
}
