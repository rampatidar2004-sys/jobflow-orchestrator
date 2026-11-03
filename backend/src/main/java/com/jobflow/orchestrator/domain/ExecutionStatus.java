package com.jobflow.orchestrator.domain;

public enum ExecutionStatus {
    PENDING,
    CLAIMED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED
}
