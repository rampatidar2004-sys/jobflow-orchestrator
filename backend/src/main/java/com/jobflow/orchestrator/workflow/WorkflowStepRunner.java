package com.jobflow.orchestrator.workflow;

import com.jobflow.orchestrator.domain.WorkflowStep;
import com.jobflow.orchestrator.domain.WorkflowStepExecution;
import com.jobflow.orchestrator.domain.WorkflowStepStatus;
import com.jobflow.orchestrator.repository.WorkflowStepExecutionRepository;
import com.jobflow.orchestrator.taskhandler.TaskExecutionContext;
import com.jobflow.orchestrator.taskhandler.TaskHandler;
import com.jobflow.orchestrator.taskhandler.TaskHandlerRegistry;
import com.jobflow.orchestrator.taskhandler.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Executes one READY workflow step on the shared bounded worker pool (the
 * same pool used by scheduled jobs — total execution concurrency stays
 * bounded system-wide), then re-runs {@link WorkflowProgressService#advance}
 * to unlock and dispatch any newly-READY downstream steps.
 */
@Service
public class WorkflowStepRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowStepRunner.class);

    private final ExecutorService jobWorkerExecutor;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowProgressService progressService;

    public WorkflowStepRunner(ExecutorService jobWorkerExecutor,
                               TaskHandlerRegistry taskHandlerRegistry,
                               WorkflowStepExecutionRepository stepExecutionRepository,
                               WorkflowProgressService progressService) {
        this.jobWorkerExecutor = jobWorkerExecutor;
        this.taskHandlerRegistry = taskHandlerRegistry;
        this.stepExecutionRepository = stepExecutionRepository;
        this.progressService = progressService;
    }

    public void dispatch(WorkflowStepExecution stepExecution, WorkflowStep stepDef,
                          Map<String, Set<String>> dependenciesByStepKey) {
        jobWorkerExecutor.submit(() -> runSafely(stepExecution.getId(), stepDef, dependenciesByStepKey));
    }

    private void runSafely(UUID stepExecutionId, WorkflowStep stepDef, Map<String, Set<String>> dependenciesByStepKey) {
        try {
            run(stepExecutionId, stepDef, dependenciesByStepKey);
        } catch (Exception e) {
            log.error("unexpected error running workflow step execution {}", stepExecutionId, e);
        }
    }

    private void run(UUID stepExecutionId, WorkflowStep stepDef, Map<String, Set<String>> dependenciesByStepKey) {
        markRunning(stepExecutionId);

        var handler = taskHandlerRegistry.find(stepDef.getTaskType());
        boolean success;
        String message;

        if (handler.isEmpty()) {
            success = false;
            message = "no TaskHandler registered for type '" + stepDef.getTaskType() + "'";
        } else {
            TaskExecutionContext context = new TaskExecutionContext(
                    null, stepExecutionId, 1, 1, stepDef.getPayload());
            try {
                TaskResult result = handler.get().execute(context);
                success = result.success();
                message = result.summary();
            } catch (Exception e) {
                success = false;
                message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            }
        }

        UUID workflowExecutionId = recordOutcome(stepExecutionId, success, message);
        progressService.advance(workflowExecutionId, dependenciesByStepKey);
    }

    @Transactional
    protected void markRunning(UUID stepExecutionId) {
        stepExecutionRepository.findById(stepExecutionId).ifPresent(se -> {
            se.setStatus(WorkflowStepStatus.RUNNING);
            se.setStartedAt(Instant.now());
            stepExecutionRepository.save(se);
        });
    }

    @Transactional
    protected UUID recordOutcome(UUID stepExecutionId, boolean success, String message) {
        WorkflowStepExecution se = stepExecutionRepository.findById(stepExecutionId).orElseThrow();
        se.setStatus(success ? WorkflowStepStatus.SUCCEEDED : WorkflowStepStatus.FAILED);
        se.setFinishedAt(Instant.now());
        stepExecutionRepository.save(se);
        log.info("workflow step execution {} ({}) finished: {} — {}",
                se.getId(), se.getStepKey(), se.getStatus(), message);
        return se.getWorkflowExecutionId();
    }
}
