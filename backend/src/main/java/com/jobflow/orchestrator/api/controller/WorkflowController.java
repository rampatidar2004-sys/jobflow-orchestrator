package com.jobflow.orchestrator.api.controller;

import com.jobflow.orchestrator.api.dto.CreateWorkflowRequest;
import com.jobflow.orchestrator.api.dto.StartWorkflowRequest;
import com.jobflow.orchestrator.domain.WorkflowDefinition;
import com.jobflow.orchestrator.domain.WorkflowExecution;
import com.jobflow.orchestrator.domain.WorkflowStepExecution;
import com.jobflow.orchestrator.workflow.WorkflowExecutionService;
import com.jobflow.orchestrator.workflow.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final WorkflowExecutionService executionService;

    public WorkflowController(WorkflowService workflowService, WorkflowExecutionService executionService) {
        this.workflowService = workflowService;
        this.executionService = executionService;
    }

    @PostMapping
    public ResponseEntity<WorkflowDefinition> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowService.create(request, null));
    }

    @GetMapping
    public List<WorkflowDefinition> list() {
        return workflowService.list();
    }

    @GetMapping("/{id}")
    public WorkflowDefinition get(@PathVariable UUID id) {
        return workflowService.get(id);
    }

    @PostMapping("/{id}/executions")
    public ResponseEntity<WorkflowExecution> start(@PathVariable UUID id,
                                                     @RequestBody(required = false) StartWorkflowRequest request) {
        String idempotencyKey = request != null ? request.idempotencyKey() : null;
        return ResponseEntity.status(HttpStatus.CREATED).body(executionService.start(id, idempotencyKey));
    }

    @GetMapping("/{id}/executions")
    public List<WorkflowExecution> listExecutions(@PathVariable UUID id) {
        return executionService.listForWorkflow(id);
    }

    @GetMapping("/executions/{executionId}")
    public WorkflowExecution getExecution(@PathVariable UUID executionId) {
        return executionService.get(executionId);
    }

    @GetMapping("/executions/{executionId}/steps")
    public List<WorkflowStepExecution> getStepExecutions(@PathVariable UUID executionId) {
        return executionService.stepExecutions(executionId);
    }
}
