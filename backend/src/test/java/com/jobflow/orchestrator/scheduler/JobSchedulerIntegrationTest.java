package com.jobflow.orchestrator.scheduler;

import com.jobflow.orchestrator.api.dto.CreateJobRequest;
import com.jobflow.orchestrator.domain.*;
import com.jobflow.orchestrator.repository.JobDefinitionRepository;
import com.jobflow.orchestrator.repository.JobExecutionRepository;
import com.jobflow.orchestrator.support.AbstractIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Exercises the real scheduler loop (the application's own @Scheduled poll
 * loop, running against a live Testcontainers Postgres) rather than calling
 * JobClaimService directly — this is what actually proves the end-to-end
 * claim -> execute -> retry -> history path works, not just its pieces.
 */
class JobSchedulerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JobService jobService;

    @Autowired
    private JobDefinitionRepository jobDefinitionRepository;

    @Autowired
    private JobExecutionRepository jobExecutionRepository;

    @Autowired
    private LeaseRecoveryService leaseRecoveryService;

    @Test
    void onceJobIsClaimedAndExecutedExactlyOnceOnHappyPath() {
        JobDefinition job = jobService.create(new CreateJobRequest(
                "happy-once", "demo", ScheduleType.ONCE, null, null, null,
                5, 3, 100L, 1000L, "{\"durationMs\":10}"
        ), null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<JobExecution> executions = jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(job.getId());
            assertThat(executions).hasSize(1);
            assertThat(executions.get(0).getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        });

        // A ONCE job must not be reclaimed after it succeeds.
        JobDefinition reloaded = jobDefinitionRepository.findById(job.getId()).orElseThrow();
        assertThat(reloaded.getNextFireAt()).isNull();
    }

    @Test
    void jobRetriesWithBackoffThenSucceeds() {
        // failUntilAttempt=2 means attempt 1 fails, attempt 2 succeeds.
        JobDefinition job = jobService.create(new CreateJobRequest(
                "retry-then-succeed", "demo", ScheduleType.ONCE, null, null, null,
                5, 5, 50L, 500L, "{\"failUntilAttempt\":2}"
        ), null);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            List<JobExecution> executions = jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(job.getId());
            assertThat(executions).hasSizeGreaterThanOrEqualTo(2);
            assertThat(executions.get(0).getStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);
        });
    }

    @Test
    void jobExhaustsMaxAttemptsAndStopsRetrying() {
        JobDefinition job = jobService.create(new CreateJobRequest(
                "always-fails", "demo", ScheduleType.ONCE, null, null, null,
                5, 2, 20L, 100L, "{\"alwaysFail\":true}"
        ), null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            List<JobExecution> executions = jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(job.getId());
            assertThat(executions).hasSize(2); // maxAttempts = 2, both FAILED
            assertThat(executions).allMatch(e -> e.getStatus() == ExecutionStatus.FAILED);
        });

        // No further execution should appear even after waiting past the backoff window.
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(6)).untilAsserted(() ->
                assertThat(jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(job.getId())).hasSize(2));
    }

    @Test
    void expiredLeaseIsRecoveredAndJobBecomesReclaimable() {
        JobDefinition job = jobService.create(new CreateJobRequest(
                "stuck-job", "demo", ScheduleType.ONCE, null, null, Instant.now().plusSeconds(3600),
                5, 3, 100L, 1000L, "{}"
        ), null);
        // Park it far in the future so the real poll loop can't claim it during this test.
        job.setNextFireAt(Instant.now().plusSeconds(3600));
        jobDefinitionRepository.save(job);

        // Simulate a worker that claimed the job and then died: CLAIMED with an
        // already-expired lease, never transitioning to RUNNING/SUCCEEDED/FAILED.
        JobExecution stuckExecution = new JobExecution(job.getId(), 1);
        stuckExecution.setStatus(ExecutionStatus.CLAIMED);
        stuckExecution.setWorkerId("worker-that-died");
        stuckExecution.setLeaseExpiresAt(Instant.now().minusSeconds(60));
        jobExecutionRepository.save(stuckExecution);

        int recovered = leaseRecoveryService.recoverBatch();
        assertThat(recovered).isEqualTo(1);

        JobExecution reloadedExecution = jobExecutionRepository.findById(stuckExecution.getId()).orElseThrow();
        assertThat(reloadedExecution.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(reloadedExecution.getErrorMessage()).contains("lease expired");

        JobDefinition reloadedJob = jobDefinitionRepository.findById(job.getId()).orElseThrow();
        assertThat(reloadedJob.getNextFireAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void completedJobPublishesALifecycleEventToKafka() {
        JobDefinition job = jobService.create(new CreateJobRequest(
                "kafka-event-job", "demo", ScheduleType.ONCE, null, null, null,
                5, 3, 50L, 500L, "{\"durationMs\":10}"
        ), null);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("job-lifecycle-events"));

            await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean sawCompletedForThisJob = false;
                for (var record : records) {
                    if (record.value() != null
                            && record.value().contains(job.getId().toString())
                            && record.value().contains("COMPLETED")) {
                        sawCompletedForThisJob = true;
                    }
                }
                assertThat(sawCompletedForThisJob).isTrue();
            });
        }
    }
}
