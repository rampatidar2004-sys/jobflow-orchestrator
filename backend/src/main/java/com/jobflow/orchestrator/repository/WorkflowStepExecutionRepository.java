package com.jobflow.orchestrator.repository;

import com.jobflow.orchestrator.domain.WorkflowStepExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowStepExecutionRepository extends JpaRepository<WorkflowStepExecution, UUID> {

    List<WorkflowStepExecution> findByWorkflowExecutionId(UUID workflowExecutionId);

    Optional<WorkflowStepExecution> findByWorkflowExecutionIdAndStepKey(UUID workflowExecutionId, String stepKey);
}
