package com.jobflow.orchestrator.api.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record CreateWorkflowStepRequest(
        @NotBlank String stepKey,
        @NotBlank String taskType,
        String payload,
        Set<String> dependsOn
) {
}
