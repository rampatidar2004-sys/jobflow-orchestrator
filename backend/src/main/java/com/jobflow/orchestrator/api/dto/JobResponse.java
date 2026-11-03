package com.jobflow.orchestrator.api.dto;

import com.jobflow.orchestrator.domain.JobDefinition;
import com.jobflow.orchestrator.domain.JobStatus;
import com.jobflow.orchestrator.domain.ScheduleType;

import java.time.Instant;
import java.util.UUID;

public record JobResponse(
        UUID id,
        String name,
        String taskType,
        ScheduleType scheduleType,
        String cronExpression,
        Long intervalSeconds,
        Instant runAt,
        int priority,
        JobStatus status,
        int maxAttempts,
        String payload,
        Instant nextFireAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static JobResponse from(JobDefinition job) {
        return new JobResponse(
                job.getId(), job.getName(), job.getTaskType(), job.getScheduleType(),
                job.getCronExpression(), job.getIntervalSeconds(), job.getRunAt(),
                job.getPriority(), job.getStatus(), job.getMaxAttempts(), job.getPayload(),
                job.getNextFireAt(), job.getCreatedAt(), job.getUpdatedAt());
    }
}
