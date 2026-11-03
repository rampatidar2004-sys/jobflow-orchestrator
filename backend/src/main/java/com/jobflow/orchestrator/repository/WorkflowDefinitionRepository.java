package com.jobflow.orchestrator.repository;

import com.jobflow.orchestrator.domain.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, UUID> {
}
