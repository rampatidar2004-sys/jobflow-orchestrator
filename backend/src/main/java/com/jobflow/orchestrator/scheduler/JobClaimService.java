package com.jobflow.orchestrator.scheduler;

import com.jobflow.orchestrator.config.SchedulerProperties;
import com.jobflow.orchestrator.domain.ExecutionStatus;
import com.jobflow.orchestrator.domain.JobDefinition;
import com.jobflow.orchestrator.domain.JobExecution;
import com.jobflow.orchestrator.repository.JobDefinitionRepository;
import com.jobflow.orchestrator.repository.JobExecutionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Claims due jobs using PostgreSQL's {@code FOR UPDATE SKIP LOCKED} so that
 * multiple scheduler instances polling concurrently never claim the same job
 * twice. PostgreSQL remains the single source of truth for what has been
 * claimed; Redis is never consulted for this decision.
 */
@Service
public class JobClaimService {

    private static final Logger log = LoggerFactory.getLogger(JobClaimService.class);

    private final JobDefinitionRepository jobDefinitionRepository;
    private final JobExecutionRepository jobExecutionRepository;
    private final NextFireCalculator nextFireCalculator;
    private final WorkerIdentity workerIdentity;
    private final SchedulerProperties props;

    public JobClaimService(JobDefinitionRepository jobDefinitionRepository,
                            JobExecutionRepository jobExecutionRepository,
                            NextFireCalculator nextFireCalculator,
                            WorkerIdentity workerIdentity,
                            SchedulerProperties props) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.jobExecutionRepository = jobExecutionRepository;
        this.nextFireCalculator = nextFireCalculator;
        this.workerIdentity = workerIdentity;
        this.props = props;
    }

    /**
     * Locks a batch of due jobs, creates a CLAIMED execution + lease for each,
     * and advances each job's next_fire_at so it isn't claimed again until the
     * next scheduled occurrence (or never, for ONCE jobs). All in one
     * transaction so the claim and the schedule advance are atomic.
     */
    @Transactional
    public List<ClaimedJob> claimDueJobs() {
        List<UUID> dueIds = jobDefinitionRepository.lockDueJobIds(props.getClaimBatchSize());
        if (dueIds.isEmpty()) {
            return List.of();
        }

        List<ClaimedJob> claimed = new ArrayList<>();
        Instant now = Instant.now();
        Instant leaseExpiry = now.plusSeconds(props.getLeaseDurationSeconds());

        for (UUID jobId : dueIds) {
            Optional<JobDefinition> maybeJob = jobDefinitionRepository.findById(jobId);
            if (maybeJob.isEmpty()) {
                continue;
            }
            JobDefinition job = maybeJob.get();

            long priorAttempts = jobExecutionRepository.countByJobIdAndStatus(jobId, ExecutionStatus.FAILED);
            int attemptNumber = (int) priorAttempts + 1;

            JobExecution execution = new JobExecution(jobId, attemptNumber);
            execution.setStatus(ExecutionStatus.CLAIMED);
            execution.setWorkerId(workerIdentity.id());
            execution.setLeaseExpiresAt(leaseExpiry);
            jobExecutionRepository.save(execution);

            // Advance the schedule now, inside the same transaction as the claim,
            // so a crashed worker never leaves the job permanently un-claimable
            // nor double-fires it before the next legitimate occurrence.
            Optional<Instant> next = nextFireCalculator.nextFireAfterExecution(job);
            job.setNextFireAt(next.orElse(null));
            jobDefinitionRepository.save(job);

            claimed.add(new ClaimedJob(job, execution));
        }

        if (!claimed.isEmpty()) {
            log.debug("worker={} claimed {} job(s)", workerIdentity.id(), claimed.size());
        }
        return claimed;
    }

    public record ClaimedJob(JobDefinition job, JobExecution execution) {
    }
}
