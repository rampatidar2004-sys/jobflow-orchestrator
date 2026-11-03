package com.jobflow.orchestrator.workflow;

import com.jobflow.orchestrator.domain.*;
import com.jobflow.orchestrator.repository.WorkflowDefinitionRepository;
import com.jobflow.orchestrator.repository.WorkflowExecutionRepository;
import com.jobflow.orchestrator.repository.WorkflowStepExecutionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Starts workflow executions. When an idempotency key is supplied and an
 * execution with that (workflow_id, idempotency_key) pair already exists —
 * enforced by a unique partial index in the schema — the existing execution
 * is returned unchanged instead of starting a duplicate run.
 */
@Service
public class WorkflowExecutionService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowProgressService progressService;
    private final WorkflowStepRunner stepRunner;

    public WorkflowExecutionService(WorkflowDefinitionRepository workflowDefinitionRepository,
                                     WorkflowExecutionRepository workflowExecutionRepository,
                                     WorkflowStepExecutionRepository stepExecutionRepository,
                                     WorkflowProgressService progressService,
                                     WorkflowStepRunner stepRunner) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.stepExecutionRepository = stepExecutionRepository;
        this.progressService = progressService;
        this.stepRunner = stepRunner;
    }

    @Transactional
    public WorkflowExecution start(UUID workflowId, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<WorkflowExecution> existing =
                    workflowExecutionRepository.findByWorkflowIdAndIdempotencyKey(workflowId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new NoSuchElementException("workflow not found: " + workflowId));

        WorkflowExecution execution;
        try {
            execution = workflowExecutionRepository.save(
                    new WorkflowExecution(workflowId, blankToNull(idempotencyKey)));
        } catch (DataIntegrityViolationException raceLoser) {
            // Two concurrent callers raced on the same idempotency key; the unique
            // index caught it. Return the row the winner created.
            return workflowExecutionRepository
                    .findByWorkflowIdAndIdempotencyKey(workflowId, idempotencyKey)
                    .orElseThrow(() -> raceLoser);
        }

        Map<String, Set<String>> dependenciesByStepKey = new HashMap<>();
        Map<String, WorkflowStep> stepDefsByKey = new HashMap<>();
        for (WorkflowStep step : workflow.getSteps()) {
            dependenciesByStepKey.put(step.getStepKey(), step.getDependsOn());
            stepDefsByKey.put(step.getStepKey(), step);
            stepExecutionRepository.save(new WorkflowStepExecution(execution.getId(), step.getStepKey()));
        }

        List<WorkflowStepExecution> initiallyReady = progressService.advance(execution.getId(), dependenciesByStepKey);
        for (WorkflowStepExecution ready : initiallyReady) {
            stepRunner.dispatch(ready, stepDefsByKey.get(ready.getStepKey()), dependenciesByStepKey);
        }

        return execution;
    }

    public WorkflowExecution get(UUID executionId) {
        return workflowExecutionRepository.findById(executionId)
                .orElseThrow(() -> new NoSuchElementException("workflow execution not found: " + executionId));
    }

    public List<WorkflowExecution> listForWorkflow(UUID workflowId) {
        return workflowExecutionRepository.findByWorkflowIdOrderByStartedAtDesc(workflowId);
    }

    public List<WorkflowStepExecution> stepExecutions(UUID executionId) {
        return stepExecutionRepository.findByWorkflowExecutionId(executionId);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
