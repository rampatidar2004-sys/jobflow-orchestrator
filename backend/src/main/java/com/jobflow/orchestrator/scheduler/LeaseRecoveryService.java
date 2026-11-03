package com.jobflow.orchestrator.scheduler;

import com.jobflow.orchestrator.config.SchedulerProperties;
import com.jobflow.orchestrator.domain.ExecutionStatus;
import com.jobflow.orchestrator.domain.JobDefinition;
import com.jobflow.orchestrator.domain.JobExecution;
import com.jobflow.orchestrator.repository.JobDefinitionRepository;
import com.jobflow.orchestrator.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Recovers executions whose lease expired while still CLAIMED/RUNNING —
 * typically because the worker holding them crashed, was killed, or was
 * network-partitioned from the database. Such executions are marked FAILED
 * (consuming an attempt) and, if attempts remain, the parent job is
 * rescheduled immediately so a healthy worker can retry it.
 */
@Service
public class LeaseRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(LeaseRecoveryService.class);

    private final JobExecutionRepository jobExecutionRepository;
    private final JobDefinitionRepository jobDefinitionRepository;
    private final SchedulerProperties props;

    public LeaseRecoveryService(JobExecutionRepository jobExecutionRepository,
                                 JobDefinitionRepository jobDefinitionRepository,
                                 SchedulerProperties props) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.props = props;
    }

    @Scheduled(fixedDelayString = "${jobflow.scheduler.lease-recovery-interval-ms:15000}")
    public void recoverExpiredLeases() {
        recoverBatch();
    }

    @Transactional
    public int recoverBatch() {
        List<UUID> expiredIds = jobExecutionRepository.lockExpiredLeaseIds(props.getClaimBatchSize());
        for (UUID executionId : expiredIds) {
            Optional<JobExecution> maybeExecution = jobExecutionRepository.findById(executionId);
            if (maybeExecution.isEmpty()) {
                continue;
            }
            JobExecution execution = maybeExecution.get();
            execution.setStatus(ExecutionStatus.FAILED);
            execution.setErrorMessage("lease expired — worker presumed lost, execution recovered");
            execution.setFinishedAt(Instant.now());
            jobExecutionRepository.save(execution);

            jobDefinitionRepository.findById(execution.getJobId()).ifPresent(job -> {
                if (execution.getAttemptNumber() < job.getMaxAttempts()) {
                    job.setNextFireAt(Instant.now());
                    jobDefinitionRepository.save(job);
                    log.warn("recovered expired lease for job={} execution={}, rescheduled immediately",
                            job.getId(), execution.getId());
                } else {
                    log.warn("recovered expired lease for job={} execution={}, max attempts exhausted, not rescheduled",
                            job.getId(), execution.getId());
                }
            });
        }
        return expiredIds.size();
    }
}
