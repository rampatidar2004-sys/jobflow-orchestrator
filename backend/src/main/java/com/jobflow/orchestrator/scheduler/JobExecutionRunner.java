package com.jobflow.orchestrator.scheduler;

import com.jobflow.orchestrator.cache.JobDetailCacheService;
import com.jobflow.orchestrator.config.SchedulerProperties;
import com.jobflow.orchestrator.domain.ExecutionStatus;
import com.jobflow.orchestrator.domain.JobDefinition;
import com.jobflow.orchestrator.domain.JobExecution;
import com.jobflow.orchestrator.messaging.JobLifecycleEventPublisher;
import com.jobflow.orchestrator.repository.JobDefinitionRepository;
import com.jobflow.orchestrator.repository.JobExecutionRepository;
import com.jobflow.orchestrator.taskhandler.TaskExecutionContext;
import com.jobflow.orchestrator.taskhandler.TaskHandler;
import com.jobflow.orchestrator.taskhandler.TaskHandlerRegistry;
import com.jobflow.orchestrator.taskhandler.TaskResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Runs one claimed job's task on the bounded worker pool, then persists the
 * outcome, applies exponential-backoff retry when attempts remain, and emits
 * lifecycle notifications. Execution itself happens outside any database
 * transaction (a TaskHandler can be slow); only the before/after state
 * transitions are transactional.
 */
@Service
public class JobExecutionRunner {

    private static final Logger log = LoggerFactory.getLogger(JobExecutionRunner.class);

    private final ExecutorService jobWorkerExecutor;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final JobExecutionRepository jobExecutionRepository;
    private final JobDefinitionRepository jobDefinitionRepository;
    private final BackoffCalculator backoffCalculator;
    private final JobLifecycleEventPublisher eventPublisher;
    private final JobDetailCacheService cacheService;
    private final SchedulerProperties props;

    public JobExecutionRunner(ExecutorService jobWorkerExecutor,
                               TaskHandlerRegistry taskHandlerRegistry,
                               JobExecutionRepository jobExecutionRepository,
                               JobDefinitionRepository jobDefinitionRepository,
                               BackoffCalculator backoffCalculator,
                               JobLifecycleEventPublisher eventPublisher,
                               JobDetailCacheService cacheService,
                               SchedulerProperties props) {
        this.jobWorkerExecutor = jobWorkerExecutor;
        this.taskHandlerRegistry = taskHandlerRegistry;
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.backoffCalculator = backoffCalculator;
        this.eventPublisher = eventPublisher;
        this.cacheService = cacheService;
        this.props = props;
    }

    public void submit(JobClaimService.ClaimedJob claimedJob) {
        jobWorkerExecutor.submit(() -> runSafely(claimedJob));
    }

    private void runSafely(JobClaimService.ClaimedJob claimedJob) {
        try {
            run(claimedJob);
        } catch (Exception unexpected) {
            log.error("unexpected error running job={} execution={}",
                    claimedJob.job().getId(), claimedJob.execution().getId(), unexpected);
        }
    }

    private void run(JobClaimService.ClaimedJob claimedJob) {
        JobDefinition job = claimedJob.job();
        JobExecution execution = claimedJob.execution();

        markRunning(execution.getId());

        Optional<TaskHandler> handler = taskHandlerRegistry.find(job.getTaskType());
        if (handler.isEmpty()) {
            recordFailure(job, execution, "no TaskHandler registered for type '" + job.getTaskType() + "'");
            return;
        }

        TaskExecutionContext context = new TaskExecutionContext(
                job.getId(), execution.getId(), execution.getAttemptNumber(), job.getMaxAttempts(), job.getPayload());

        try {
            TaskResult result = handler.get().execute(context);
            if (result.success()) {
                recordSuccess(job, execution, result.summary());
            } else {
                recordFailure(job, execution, result.summary());
            }
        } catch (Exception e) {
            recordFailure(job, execution, e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    @Transactional
    protected void markRunning(java.util.UUID executionId) {
        jobExecutionRepository.findById(executionId).ifPresent(exec -> {
            exec.setStatus(ExecutionStatus.RUNNING);
            exec.setStartedAt(Instant.now());
            jobExecutionRepository.save(exec);
        });
    }

    @Transactional
    protected void recordSuccess(JobDefinition job, JobExecution execution, String summary) {
        execution.setStatus(ExecutionStatus.SUCCEEDED);
        execution.setFinishedAt(Instant.now());
        execution.setResultSummary(summary);
        jobExecutionRepository.save(execution);
        cacheService.evict(job.getId());
        eventPublisher.completed(job.getId(), execution.getId(), execution.getAttemptNumber(), summary);
        log.info("job={} execution={} succeeded on attempt {}: {}",
                job.getId(), execution.getId(), execution.getAttemptNumber(), summary);
    }

    @Transactional
    protected void recordFailure(JobDefinition job, JobExecution execution, String errorMessage) {
        execution.setStatus(ExecutionStatus.FAILED);
        execution.setFinishedAt(Instant.now());
        execution.setErrorMessage(errorMessage);
        jobExecutionRepository.save(execution);
        cacheService.evict(job.getId());

        if (execution.getAttemptNumber() < job.getMaxAttempts()) {
            long delayMs = backoffCalculator.delayMillis(
                    job.getBaseBackoffMs(), job.getMaxBackoffMs(), execution.getAttemptNumber());
            job.setNextFireAt(Instant.now().plusMillis(delayMs));
            jobDefinitionRepository.save(job);
            eventPublisher.retryScheduled(job.getId(), execution.getId(), execution.getAttemptNumber(), errorMessage);
            log.warn("job={} execution={} failed on attempt {} — retry in {}ms: {}",
                    job.getId(), execution.getId(), execution.getAttemptNumber(), delayMs, errorMessage);
        } else {
            eventPublisher.failed(job.getId(), execution.getId(), execution.getAttemptNumber(), errorMessage);
            log.error("job={} execution={} exhausted {} attempts, giving up: {}",
                    job.getId(), execution.getId(), job.getMaxAttempts(), errorMessage);
        }
    }
}
