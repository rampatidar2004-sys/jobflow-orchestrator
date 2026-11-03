package com.jobflow.orchestrator.api.controller;

import com.jobflow.orchestrator.api.dto.CreateJobRequest;
import com.jobflow.orchestrator.api.dto.JobResponse;
import com.jobflow.orchestrator.api.dto.UpdateJobRequest;
import com.jobflow.orchestrator.domain.JobExecution;
import com.jobflow.orchestrator.repository.JobExecutionRepository;
import com.jobflow.orchestrator.scheduler.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final JobService jobService;
    private final JobExecutionRepository jobExecutionRepository;

    public JobController(JobService jobService, JobExecutionRepository jobExecutionRepository) {
        this.jobService = jobService;
        this.jobExecutionRepository = jobExecutionRepository;
    }

    @PostMapping
    public ResponseEntity<JobResponse> create(@Valid @RequestBody CreateJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(jobService.create(request, null)));
    }

    @GetMapping
    public List<JobResponse> list() {
        return jobService.list().stream().map(JobResponse::from).toList();
    }

    @GetMapping("/{id}")
    public JobResponse get(@PathVariable UUID id) {
        return JobResponse.from(jobService.get(id));
    }

    @PutMapping("/{id}")
    public JobResponse update(@PathVariable UUID id, @RequestBody UpdateJobRequest request) {
        return JobResponse.from(jobService.update(id, request));
    }

    @PostMapping("/{id}/pause")
    public JobResponse pause(@PathVariable UUID id) {
        return JobResponse.from(jobService.pause(id));
    }

    @PostMapping("/{id}/resume")
    public JobResponse resume(@PathVariable UUID id) {
        return JobResponse.from(jobService.resume(id));
    }

    @PostMapping("/{id}/run")
    public JobResponse runNow(@PathVariable UUID id) {
        return JobResponse.from(jobService.runNow(id));
    }

    @PostMapping("/{id}/cancel")
    public JobResponse cancel(@PathVariable UUID id) {
        return JobResponse.from(jobService.cancel(id));
    }

    @GetMapping("/{id}/executions")
    public List<JobExecution> executions(@PathVariable UUID id) {
        return jobExecutionRepository.findByJobIdOrderByCreatedAtDesc(id);
    }
}
