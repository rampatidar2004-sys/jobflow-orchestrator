package com.jobflow.orchestrator.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Publishes job lifecycle events (completion, retry, failure) to Kafka.
 * Kafka is a notification channel only — it is never read back to determine
 * scheduling correctness, which always lives in PostgreSQL.
 */
@Component
public class JobLifecycleEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(JobLifecycleEventPublisher.class);
    public static final String TOPIC = "job-lifecycle-events";

    private final KafkaTemplate<String, JobLifecycleEvent> kafkaTemplate;

    public JobLifecycleEventPublisher(KafkaTemplate<String, JobLifecycleEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void completed(UUID jobId, UUID executionId, int attemptNumber, String message) {
        publish(jobId, executionId, "COMPLETED", attemptNumber, message);
    }

    public void retryScheduled(UUID jobId, UUID executionId, int attemptNumber, String message) {
        publish(jobId, executionId, "RETRY", attemptNumber, message);
    }

    public void failed(UUID jobId, UUID executionId, int attemptNumber, String message) {
        publish(jobId, executionId, "FAILED", attemptNumber, message);
    }

    private void publish(UUID jobId, UUID executionId, String eventType, int attemptNumber, String message) {
        JobLifecycleEvent event = new JobLifecycleEvent(
                jobId, executionId, eventType, attemptNumber, message, Instant.now());
        try {
            kafkaTemplate.send(TOPIC, jobId.toString(), event);
        } catch (Exception e) {
            // Kafka is best-effort notification; a publish failure must never fail
            // or roll back the durable scheduling decision already committed to Postgres.
            log.warn("failed to publish {} event for job={} execution={}: {}",
                    eventType, jobId, executionId, e.getMessage());
        }
    }
}
