package com.jobflow.orchestrator.workflow;

import com.jobflow.orchestrator.domain.*;
import com.jobflow.orchestrator.repository.WorkflowExecutionRepository;
import com.jobflow.orchestrator.repository.WorkflowStepExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * Advances a workflow execution's step graph. Called after a step finishes
 * (success or failure) and once when the execution starts. Each call:
 * <ol>
 *   <li>marks any PENDING step whose dependencies all SUCCEEDED as READY</li>
 *   <li>marks any PENDING step with a FAILED or SKIPPED dependency as SKIPPED
 *       (this cascades transitively through the graph within the same call)</li>
 *   <li>finalizes the workflow execution once no step is left PENDING/READY/RUNNING</li>
 * </ol>
 * The whole method runs inside a transaction that first locks the parent
 * workflow_executions row, serializing concurrent advances triggered by
 * sibling steps completing on different worker threads at the same time.
 */
@Service
public class WorkflowProgressService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowProgressService.class);

    private final WorkflowExecutionRepository workflowExecutionRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;

    public WorkflowProgressService(WorkflowExecutionRepository workflowExecutionRepository,
                                    WorkflowStepExecutionRepository stepExecutionRepository) {
        this.workflowExecutionRepository = workflowExecutionRepository;
        this.stepExecutionRepository = stepExecutionRepository;
    }

    @Transactional
    public List<WorkflowStepExecution> advance(UUID workflowExecutionId, Map<String, Set<String>> dependenciesByStepKey) {
        workflowExecutionRepository.lockForAdvance(workflowExecutionId);

        List<WorkflowStepExecution> stepExecutions = stepExecutionRepository.findByWorkflowExecutionId(workflowExecutionId);
        Map<String, WorkflowStepExecution> byKey = new HashMap<>();
        for (WorkflowStepExecution se : stepExecutions) {
            byKey.put(se.getStepKey(), se);
        }

        List<WorkflowStepExecution> newlyReady = new ArrayList<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (WorkflowStepExecution se : stepExecutions) {
                if (se.getStatus() != WorkflowStepStatus.PENDING) {
                    continue;
                }
                Set<String> deps = dependenciesByStepKey.getOrDefault(se.getStepKey(), Set.of());
                boolean anyFailedOrSkipped = false;
                boolean allSucceeded = true;
                for (String dep : deps) {
                    WorkflowStepExecution depExec = byKey.get(dep);
                    if (depExec == null) continue;
                    if (depExec.getStatus() == WorkflowStepStatus.FAILED || depExec.getStatus() == WorkflowStepStatus.SKIPPED) {
                        anyFailedOrSkipped = true;
                    }
                    if (depExec.getStatus() != WorkflowStepStatus.SUCCEEDED) {
                        allSucceeded = false;
                    }
                }
                if (anyFailedOrSkipped) {
                    se.setStatus(WorkflowStepStatus.SKIPPED);
                    se.setFinishedAt(Instant.now());
                    stepExecutionRepository.save(se);
                    changed = true;
                } else if (allSucceeded) {
                    se.setStatus(WorkflowStepStatus.READY);
                    stepExecutionRepository.save(se);
                    newlyReady.add(se);
                    changed = true;
                }
            }
        }

        boolean anyUnfinished = stepExecutions.stream().anyMatch(se ->
                se.getStatus() == WorkflowStepStatus.PENDING
                        || se.getStatus() == WorkflowStepStatus.READY
                        || se.getStatus() == WorkflowStepStatus.RUNNING);

        if (!anyUnfinished) {
            boolean anyFailed = stepExecutions.stream().anyMatch(se -> se.getStatus() == WorkflowStepStatus.FAILED);
            workflowExecutionRepository.findById(workflowExecutionId).ifPresent(exec -> {
                exec.setStatus(anyFailed ? WorkflowExecutionStatus.FAILED : WorkflowExecutionStatus.SUCCEEDED);
                exec.setFinishedAt(Instant.now());
                workflowExecutionRepository.save(exec);
                log.info("workflow execution {} finished with status {}", workflowExecutionId, exec.getStatus());
            });
        }

        return newlyReady;
    }
}
