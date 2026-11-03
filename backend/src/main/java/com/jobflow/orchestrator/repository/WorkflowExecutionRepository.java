package com.jobflow.orchestrator.repository;

import com.jobflow.orchestrator.domain.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, UUID> {

    Optional<WorkflowExecution> findByWorkflowIdAndIdempotencyKey(UUID workflowId, String idempotencyKey);

    List<WorkflowExecution> findByWorkflowIdOrderByStartedAtDesc(UUID workflowId);

    /**
     * Locks the workflow_executions row itself so that concurrent completions
     * of sibling steps (each running on a different worker thread) serialize
     * their progress-advance logic instead of racing on the same execution.
     */
    @Query(value = "SELECT id FROM workflow_executions WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<UUID> lockForAdvance(@Param("id") UUID id);
}
