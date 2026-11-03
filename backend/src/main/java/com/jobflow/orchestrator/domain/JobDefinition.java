package com.jobflow.orchestrator.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_definitions")
public class JobDefinition {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Bean/qualifier name of the TaskHandler that executes this job. */
    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private ScheduleType scheduleType;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "interval_seconds")
    private Long intervalSeconds;

    @Column(name = "run_at")
    private Instant runAt;

    @Column(nullable = false)
    private int priority = 5;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 5;

    @Column(name = "base_backoff_ms", nullable = false)
    private long baseBackoffMs = 1000;

    @Column(name = "max_backoff_ms", nullable = false)
    private long maxBackoffMs = 300_000;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload = "{}";

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected JobDefinition() {
        // JPA
    }

    public JobDefinition(String name, String taskType, ScheduleType scheduleType) {
        this.name = name;
        this.taskType = taskType;
        this.scheduleType = scheduleType;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // --- getters / setters ---

    public UUID getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public ScheduleType getScheduleType() { return scheduleType; }
    public void setScheduleType(ScheduleType scheduleType) { this.scheduleType = scheduleType; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public Long getIntervalSeconds() { return intervalSeconds; }
    public void setIntervalSeconds(Long intervalSeconds) { this.intervalSeconds = intervalSeconds; }

    public Instant getRunAt() { return runAt; }
    public void setRunAt(Instant runAt) { this.runAt = runAt; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public long getBaseBackoffMs() { return baseBackoffMs; }
    public void setBaseBackoffMs(long baseBackoffMs) { this.baseBackoffMs = baseBackoffMs; }

    public long getMaxBackoffMs() { return maxBackoffMs; }
    public void setMaxBackoffMs(long maxBackoffMs) { this.maxBackoffMs = maxBackoffMs; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Instant getNextFireAt() { return nextFireAt; }
    public void setNextFireAt(Instant nextFireAt) { this.nextFireAt = nextFireAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
