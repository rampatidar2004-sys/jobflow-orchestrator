package com.jobflow.orchestrator.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.jobflow.orchestrator.workflow.WorkflowDagValidator.StepSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowDagValidatorTest {

    private final WorkflowDagValidator validator = new WorkflowDagValidator();

    @Test
    void acceptsTheDocumentedPrepareValidateEnrichFinishGraph() {
        List<StepSpec> steps = List.of(
                new StepSpec("prepare", Set.of()),
                new StepSpec("validate", Set.of("prepare")),
                new StepSpec("enrich", Set.of("prepare")),
                new StepSpec("finish", Set.of("validate", "enrich"))
        );

        List<String> order = validator.validateAndTopoSort(steps);

        assertThat(order).hasSize(4);
        assertThat(order.indexOf("prepare")).isLessThan(order.indexOf("validate"));
        assertThat(order.indexOf("prepare")).isLessThan(order.indexOf("enrich"));
        assertThat(order.indexOf("validate")).isLessThan(order.indexOf("finish"));
        assertThat(order.indexOf("enrich")).isLessThan(order.indexOf("finish"));
    }

    @Test
    void rejectsDuplicateStepKeys() {
        List<StepSpec> steps = List.of(
                new StepSpec("a", Set.of()),
                new StepSpec("a", Set.of())
        );

        assertThatThrownBy(() -> validator.validateAndTopoSort(steps))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class)
                .hasMessageContaining("duplicate step key");
    }

    @Test
    void rejectsUnknownDependency() {
        List<StepSpec> steps = List.of(
                new StepSpec("a", Set.of("ghost"))
        );

        assertThatThrownBy(() -> validator.validateAndTopoSort(steps))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class)
                .hasMessageContaining("unknown step");
    }

    @Test
    void rejectsSelfDependency() {
        List<StepSpec> steps = List.of(new StepSpec("a", Set.of("a")));

        assertThatThrownBy(() -> validator.validateAndTopoSort(steps))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class)
                .hasMessageContaining("cannot depend on itself");
    }

    @Test
    void rejectsATwoStepCycle() {
        List<StepSpec> steps = List.of(
                new StepSpec("a", Set.of("b")),
                new StepSpec("b", Set.of("a"))
        );

        assertThatThrownBy(() -> validator.validateAndTopoSort(steps))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class)
                .hasMessageContaining("cycle");
    }

    @Test
    void rejectsALongerCycleThatDoesNotIncludeEveryNode() {
        List<StepSpec> steps = List.of(
                new StepSpec("start", Set.of()),
                new StepSpec("a", Set.of("start", "c")),
                new StepSpec("b", Set.of("a")),
                new StepSpec("c", Set.of("b"))
        );

        assertThatThrownBy(() -> validator.validateAndTopoSort(steps))
                .isInstanceOf(WorkflowDagValidator.WorkflowValidationException.class)
                .hasMessageContaining("cycle");
    }
}
