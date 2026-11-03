package com.jobflow.orchestrator.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_step_executions")
public class WorkflowStepExecution {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "workflow_execution_id", nullable = false)
    private UUID workflowExecutionId;

    @Column(name = "step_key", nullable = false)
    private String stepKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStepStatus status = WorkflowStepStatus.PENDING;

    @Column(name = "job_execution_id")
    private UUID jobExecutionId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected WorkflowStepExecution() {
        // JPA
    }

    public WorkflowStepExecution(UUID workflowExecutionId, String stepKey) {
        this.workflowExecutionId = workflowExecutionId;
        this.stepKey = stepKey;
    }

    public UUID getId() { return id; }

    public UUID getWorkflowExecutionId() { return workflowExecutionId; }

    public String getStepKey() { return stepKey; }

    public WorkflowStepStatus getStatus() { return status; }
    public void setStatus(WorkflowStepStatus status) { this.status = status; }

    public UUID getJobExecutionId() { return jobExecutionId; }
    public void setJobExecutionId(UUID jobExecutionId) { this.jobExecutionId = jobExecutionId; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
}
