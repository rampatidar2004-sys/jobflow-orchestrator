package com.jobflow.orchestrator.domain;

public enum WorkflowStepStatus {
    PENDING,
    READY,
    RUNNING,
    SUCCEEDED,
    FAILED,
    SKIPPED
}
