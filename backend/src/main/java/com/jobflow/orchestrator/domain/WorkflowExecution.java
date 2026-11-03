package com.jobflow.orchestrator.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_executions")
public class WorkflowExecution {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowExecutionStatus status = WorkflowExecutionStatus.RUNNING;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected WorkflowExecution() {
        // JPA
    }

    public WorkflowExecution(UUID workflowId, String idempotencyKey) {
        this.workflowId = workflowId;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }

    public UUID getWorkflowId() { return workflowId; }

    public String getIdempotencyKey() { return idempotencyKey; }

    public WorkflowExecutionStatus getStatus() { return status; }
    public void setStatus(WorkflowExecutionStatus status) { this.status = status; }

    public Instant getStartedAt() { return startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
