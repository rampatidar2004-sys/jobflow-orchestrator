package com.jobflow.orchestrator.api.dto;

public record UpdateJobRequest(
        String name,
        Integer priority,
        Integer maxAttempts,
        Long baseBackoffMs,
        Long maxBackoffMs,
        String payload
) {
}
