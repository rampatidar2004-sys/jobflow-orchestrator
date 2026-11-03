package com.jobflow.orchestrator.scheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.jobflow.orchestrator.domain.JobDefinition;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Computes {@code next_fire_at} for a job based on its {@code schedule_type}.
 * ONCE jobs fire exactly once at {@code run_at} and are never rescheduled
 * (their next_fire_at is cleared to null after the first successful claim).
 */
@Component
public class NextFireCalculator {

    private final CronParser cronParser =
            new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    public Instant initialFireTime(JobDefinition job) {
        return switch (job.getScheduleType()) {
            case ONCE -> job.getRunAt() != null ? job.getRunAt() : Instant.now();
            case INTERVAL -> Instant.now();
            case CRON -> nextCronFireAfter(job.getCronExpression(), Instant.now())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "cron expression has no future execution: " + job.getCronExpression()));
        };
    }

    /**
     * Next fire time after a job has just been claimed and executed. Returns
     * empty for ONCE jobs, meaning the job will not fire again.
     */
    public Optional<Instant> nextFireAfterExecution(JobDefinition job) {
        return switch (job.getScheduleType()) {
            case ONCE -> Optional.empty();
            case INTERVAL -> Optional.of(Instant.now().plusSeconds(
                    job.getIntervalSeconds() != null ? job.getIntervalSeconds() : 60));
            case CRON -> nextCronFireAfter(job.getCronExpression(), Instant.now());
        };
    }

    private Optional<Instant> nextCronFireAfter(String cronExpression, Instant after) {
        Cron cron = cronParser.parse(cronExpression);
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(after, ZoneOffset.UTC);
        return executionTime.nextExecution(zdt).map(ZonedDateTime::toInstant);
    }
}
