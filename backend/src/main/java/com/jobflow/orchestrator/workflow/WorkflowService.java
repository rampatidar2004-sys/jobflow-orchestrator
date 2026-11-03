package com.jobflow.orchestrator.workflow;

import com.jobflow.orchestrator.api.dto.CreateWorkflowRequest;
import com.jobflow.orchestrator.api.dto.CreateWorkflowStepRequest;
import com.jobflow.orchestrator.domain.WorkflowDefinition;
import com.jobflow.orchestrator.domain.WorkflowStep;
import com.jobflow.orchestrator.repository.WorkflowDefinitionRepository;
import com.jobflow.orchestrator.taskhandler.TaskHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowDagValidator dagValidator;
    private final TaskHandlerRegistry taskHandlerRegistry;

    public WorkflowService(WorkflowDefinitionRepository workflowDefinitionRepository,
                            WorkflowDagValidator dagValidator,
                            TaskHandlerRegistry taskHandlerRegistry) {
        this.workflowDefinitionRepository = workflowDefinitionRepository;
        this.dagValidator = dagValidator;
        this.taskHandlerRegistry = taskHandlerRegistry;
    }

    @Transactional
    public WorkflowDefinition create(CreateWorkflowRequest request, UUID createdBy) {
        List<WorkflowDagValidator.StepSpec> specs = request.steps().stream()
                .map(s -> new WorkflowDagValidator.StepSpec(
                        s.stepKey(), s.dependsOn() != null ? s.dependsOn() : Set.of()))
                .toList();

        // Throws WorkflowValidationException on duplicate keys, unknown deps, or a cycle.
        dagValidator.validateAndTopoSort(specs);

        for (CreateWorkflowStepRequest step : request.steps()) {
            if (!taskHandlerRegistry.isKnownType(step.taskType())) {
                throw new WorkflowDagValidator.WorkflowValidationException(
                        "step '" + step.stepKey() + "' references unknown task type '" + step.taskType() + "'");
            }
        }

        WorkflowDefinition workflow = new WorkflowDefinition(request.name());
        workflow.setCreatedBy(createdBy);
        for (CreateWorkflowStepRequest step : request.steps()) {
            workflow.addStep(new WorkflowStep(
                    step.stepKey(), step.taskType(), step.payload(), step.dependsOn()));
        }

        return workflowDefinitionRepository.save(workflow);
    }

    public WorkflowDefinition get(UUID id) {
        return workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new java.util.NoSuchElementException("workflow not found: " + id));
    }

    public List<WorkflowDefinition> list() {
        return workflowDefinitionRepository.findAll();
    }
}
