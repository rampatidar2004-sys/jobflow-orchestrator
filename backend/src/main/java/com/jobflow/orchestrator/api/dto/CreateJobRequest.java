package com.jobflow.orchestrator.api.dto;

import com.jobflow.orchestrator.domain.ScheduleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateJobRequest(
        @NotBlank String name,
        @NotBlank String taskType,
        @NotNull ScheduleType scheduleType,
        String cronExpression,
        Long intervalSeconds,
        Instant runAt,
        @Min(1) Integer priority,
        @Min(1) Integer maxAttempts,
        Long baseBackoffMs,
        Long maxBackoffMs,
        String payload
) {
}
