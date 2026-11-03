package com.jobflow.orchestrator.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateWorkflowRequest(
        @NotBlank String name,
        @NotEmpty @Valid List<CreateWorkflowStepRequest> steps
) {
}
