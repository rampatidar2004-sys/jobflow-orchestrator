package com.jobflow.orchestrator.taskhandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DemoTaskHandlerTest {

    private final DemoTaskHandler handler = new DemoTaskHandler(new ObjectMapper());

    private TaskExecutionContext context(int attempt, String payloadJson) {
        return new TaskExecutionContext(UUID.randomUUID(), UUID.randomUUID(), attempt, 5, payloadJson);
    }

    @Test
    void succeedsByDefaultWithNoPayload() throws Exception {
        TaskResult result = handler.execute(context(1, "{}"));
        assertThat(result.success()).isTrue();
    }

    @Test
    void failsUntilTheConfiguredAttemptThenSucceeds() {
        assertThatThrownBy(() -> handler.execute(context(1, "{\"failUntilAttempt\":3}")))
                .isInstanceOf(TaskExecutionException.class);
        assertThatThrownBy(() -> handler.execute(context(2, "{\"failUntilAttempt\":3}")))
                .isInstanceOf(TaskExecutionException.class);
    }

    @Test
    void succeedsOnceAttemptReachesFailUntilAttempt() throws Exception {
        TaskResult result = handler.execute(context(3, "{\"failUntilAttempt\":3}"));
        assertThat(result.success()).isTrue();
    }

    @Test
    void alwaysFailIgnoresAttemptNumber() {
        assertThatThrownBy(() -> handler.execute(context(99, "{\"alwaysFail\":true}")))
                .isInstanceOf(TaskExecutionException.class);
    }

    @Test
    void taskTypeIsStableIdentifier() {
        assertThat(handler.taskType()).isEqualTo("demo");
    }
}
