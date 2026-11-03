package com.jobflow.orchestrator.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Polls for due jobs at a fixed interval, claims them (SKIP LOCKED), and
 * hands each claimed job to the bounded worker pool for execution. This is
 * the entry point that makes the scheduler self-running once the
 * application context is up.
 */
@Component
public class SchedulerPollLoop {

    private static final Logger log = LoggerFactory.getLogger(SchedulerPollLoop.class);

    private final JobClaimService jobClaimService;
    private final JobExecutionRunner jobExecutionRunner;

    public SchedulerPollLoop(JobClaimService jobClaimService, JobExecutionRunner jobExecutionRunner) {
        this.jobClaimService = jobClaimService;
        this.jobExecutionRunner = jobExecutionRunner;
    }

    @Scheduled(fixedDelayString = "${jobflow.scheduler.poll-interval-ms:1000}")
    public void pollAndDispatch() {
        List<JobClaimService.ClaimedJob> claimed;
        try {
            claimed = jobClaimService.claimDueJobs();
        } catch (Exception e) {
            log.error("job claim cycle failed", e);
            return;
        }
        for (JobClaimService.ClaimedJob claimedJob : claimed) {
            jobExecutionRunner.submit(claimedJob);
        }
    }
}
