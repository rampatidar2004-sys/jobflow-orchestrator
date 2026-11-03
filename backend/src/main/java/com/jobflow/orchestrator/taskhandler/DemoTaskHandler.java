package com.jobflow.orchestrator.taskhandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Deterministic handler used to demonstrate and test scheduler behavior
 * without any external side effects.
 *
 * Payload options (all optional):
 * <pre>{@code
 * {
 *   "durationMs": 200,          // how long execute() sleeps before returning
 *   "failUntilAttempt": 3,      // fail every attempt strictly before this one, then succeed
 *   "alwaysFail": false         // if true, every attempt fails (exhausts retries deterministically)
 * }
 * }</pre>
 */
@Component
public class DemoTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(DemoTaskHandler.class);
    public static final String TYPE = "demo";

    private final ObjectMapper objectMapper;

    public DemoTaskHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String taskType() {
        return TYPE;
    }

    @Override
    public TaskResult execute(TaskExecutionContext context) throws Exception {
        JsonNode payload = objectMapper.readTree(
                context.payloadJson() == null || context.payloadJson().isBlank() ? "{}" : context.payloadJson());

        long durationMs = payload.path("durationMs").asLong(0);
        int failUntilAttempt = payload.path("failUntilAttempt").asInt(0);
        boolean alwaysFail = payload.path("alwaysFail").asBoolean(false);

        if (durationMs > 0) {
            Thread.sleep(durationMs);
        }

        if (alwaysFail || context.attemptNumber() < failUntilAttempt) {
            String msg = "demo handler intentional failure on attempt " + context.attemptNumber();
            log.info("job={} execution={} {}", context.jobId(), context.executionId(), msg);
            throw new TaskExecutionException(msg);
        }

        String summary = "demo handler completed on attempt " + context.attemptNumber()
                + " after " + durationMs + "ms";
        log.info("job={} execution={} {}", context.jobId(), context.executionId(), summary);
        return TaskResult.success(summary);
    }
}
