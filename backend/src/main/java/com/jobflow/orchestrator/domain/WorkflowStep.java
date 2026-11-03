package com.jobflow.orchestrator.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workflow_steps")
public class WorkflowStep {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id", nullable = false)
    private WorkflowDefinition workflow;

    @Column(name = "step_key", nullable = false)
    private String stepKey;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload = "{}";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "workflow_step_dependencies", joinColumns = @JoinColumn(name = "workflow_step_id"))
    @Column(name = "depends_on_key")
    private Set<String> dependsOn = new HashSet<>();

    protected WorkflowStep() {
        // JPA
    }

    public WorkflowStep(String stepKey, String taskType, String payload, Set<String> dependsOn) {
        this.stepKey = stepKey;
        this.taskType = taskType;
        this.payload = payload != null ? payload : "{}";
        this.dependsOn = dependsOn != null ? new HashSet<>(dependsOn) : new HashSet<>();
    }

    public UUID getId() { return id; }

    public WorkflowDefinition getWorkflow() { return workflow; }
    public void setWorkflow(WorkflowDefinition workflow) { this.workflow = workflow; }

    public String getStepKey() { return stepKey; }

    public String getTaskType() { return taskType; }

    public String getPayload() { return payload; }

    public Set<String> getDependsOn() { return dependsOn; }
}
