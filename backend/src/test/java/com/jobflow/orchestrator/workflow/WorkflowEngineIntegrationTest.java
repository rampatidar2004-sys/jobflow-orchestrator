package com.jobflow.orchestrator.workflow;

import com.jobflow.orchestrator.api.dto.CreateWorkflowRequest;
import com.jobflow.orchestrator.api.dto.CreateWorkflowStepRequest;
import com.jobflow.orchestrator.domain.WorkflowDefinition;
import com.jobflow.orchestrator.domain.WorkflowExecution;
import com.jobflow.orchestrator.domain.WorkflowExecutionStatus;
import com.jobflow.orchestrator.domain.WorkflowStepStatus;
import com.jobflow.orchestrator.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

class WorkflowEngineIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowExecutionService executionService;

    private CreateWorkflowRequest prepareValidateEnrichFinish(String name) {
        return new CreateWorkflowRequest(name, List.of(
                new CreateWorkflowStepRequest("prepare", "demo", "{}", Set.of()),
                new CreateWorkflowStepRequest("validate", "demo", "{}", Set.of("prepare")),
                new CreateWorkflowStepRequest("enrich", "demo", "{}", Set.of("prepare")),
                new CreateWorkflowStepRequest("finish", "demo", "{}", Set.of("validate", "enrich"))
        ));
    }

    @Test
    void finishStepOnlyRunsAfterBothValidateAndEnrichSucceed() {
        WorkflowDefinition workflow = workflowService.create(
                prepareValidateEnrichFinish("prepare-validate-enrich-finish"), null);

        WorkflowExecution execution = executionService.start(workflow.getId(), null);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            WorkflowExecution reloaded = executionService.get(execution.getId());
            assertThat(reloaded.getStatus()).isEqualTo(WorkflowExecutionStatus.SUCCEEDED);
        });

        var stepStatuses = executionService.stepExecutions(execution.getId()).stream()
                .collect(Collectors.toMap(s -> s.getStepKey(), s -> s.getStatus()));

        assertThat(stepStatuses)
                .containsEntry("prepare", WorkflowStepStatus.SUCCEEDED)
                .containsEntry("validate", WorkflowStepStatus.SUCCEEDED)
                .containsEntry("enrich", WorkflowStepStatus.SUCCEEDED)
                .containsEntry("finish", WorkflowStepStatus.SUCCEEDED);

        var byKey = executionService.stepExecutions(execution.getId()).stream()
                .collect(Collectors.toMap(s -> s.getStepKey(), s -> s));

        // finish must not have started before both its dependencies finished.
        assertThat(byKey.get("finish").getStartedAt()).isAfterOrEqualTo(byKey.get("validate").getFinishedAt());
        assertThat(byKey.get("finish").getStartedAt()).isAfterOrEqualTo(byKey.get("enrich").getFinishedAt());
    }

    @Test
    void aFailedDependencySkipsDownstreamSteps() {
        CreateWorkflowRequest request = new CreateWorkflowRequest("fail-and-skip", List.of(
                new CreateWorkflowStepRequest("prepare", "demo", "{\"alwaysFail\":true}", Set.of()),
                new CreateWorkflowStepRequest("finish", "demo", "{}", Set.of("prepare"))
        ));
        WorkflowDefinition workflow = workflowService.create(request, null);
        WorkflowExecution execution = executionService.start(workflow.getId(), null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            WorkflowExecution reloaded = executionService.get(execution.getId());
            assertThat(reloaded.getStatus()).isEqualTo(WorkflowExecutionStatus.FAILED);
        });

        var byKey = executionService.stepExecutions(execution.getId()).stream()
                .collect(Collectors.toMap(s -> s.getStepKey(), s -> s.getStatus()));
        assertThat(byKey.get("prepare")).isEqualTo(WorkflowStepStatus.FAILED);
        assertThat(byKey.get("finish")).isEqualTo(WorkflowStepStatus.SKIPPED);
    }

    @Test
    void startingWithTheSameIdempotencyKeyTwiceReturnsTheSameExecution() {
        WorkflowDefinition workflow = workflowService.create(
                prepareValidateEnrichFinish("idempotent-workflow"), null);

        WorkflowExecution first = executionService.start(workflow.getId(), "run-2026-09-24");
        WorkflowExecution second = executionService.start(workflow.getId(), "run-2026-09-24");

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void creatingAWorkflowWithACycleIsRejected() {
        CreateWorkflowRequest request = new CreateWorkflowRequest("bad-cycle", List.of(
                new CreateWorkflowStepRequest("a", "demo", "{}", Set.of("b")),
                new CreateWorkflowStepRequest("b", "demo", "{}", Set.of("a"))
        ));

        assertThatThrownBy(() -> workflowService.create(request, null))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class);
    }

    @Test
    void creatingAWorkflowWithAnUnknownDependencyIsRejected() {
        CreateWorkflowRequest request = new CreateWorkflowRequest("bad-dep", List.of(
                new CreateWorkflowStepRequest("a", "demo", "{}", Set.of("ghost"))
        ));

        assertThatThrownBy(() -> workflowService.create(request, null))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class);
    }
}
