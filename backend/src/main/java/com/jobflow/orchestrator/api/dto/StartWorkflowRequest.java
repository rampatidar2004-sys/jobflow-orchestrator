package com.jobflow.orchestrator.api.dto;

public record StartWorkflowRequest(String idempotencyKey) {
}
