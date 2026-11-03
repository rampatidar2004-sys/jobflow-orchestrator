package com.jobflow.orchestrator.scheduler;

import com.jobflow.orchestrator.api.dto.CreateJobRequest;
import com.jobflow.orchestrator.api.dto.UpdateJobRequest;
import com.jobflow.orchestrator.cache.JobDetailCacheService;
import com.jobflow.orchestrator.domain.JobDefinition;
import com.jobflow.orchestrator.domain.JobStatus;
import com.jobflow.orchestrator.repository.JobDefinitionRepository;
import com.jobflow.orchestrator.taskhandler.TaskHandlerRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
public class JobService {

    private final JobDefinitionRepository jobDefinitionRepository;
    private final NextFireCalculator nextFireCalculator;
    private final TaskHandlerRegistry taskHandlerRegistry;
    private final JobDetailCacheService cacheService;

    public JobService(JobDefinitionRepository jobDefinitionRepository,
                       NextFireCalculator nextFireCalculator,
                       TaskHandlerRegistry taskHandlerRegistry,
                       JobDetailCacheService cacheService) {
        this.jobDefinitionRepository = jobDefinitionRepository;
        this.nextFireCalculator = nextFireCalculator;
        this.taskHandlerRegistry = taskHandlerRegistry;
        this.cacheService = cacheService;
    }

    @Transactional
    public JobDefinition create(CreateJobRequest request, UUID createdBy) {
        if (!taskHandlerRegistry.isKnownType(request.taskType())) {
            throw new IllegalArgumentException("unknown task type: '" + request.taskType() + "'");
        }
        JobDefinition job = new JobDefinition(request.name(), request.taskType(), request.scheduleType());
        job.setCronExpression(request.cronExpression());
        job.setIntervalSeconds(request.intervalSeconds());
        job.setRunAt(request.runAt());
        if (request.priority() != null) job.setPriority(request.priority());
        if (request.maxAttempts() != null) job.setMaxAttempts(request.maxAttempts());
        if (request.baseBackoffMs() != null) job.setBaseBackoffMs(request.baseBackoffMs());
        if (request.maxBackoffMs() != null) job.setMaxBackoffMs(request.maxBackoffMs());
        if (request.payload() != null) job.setPayload(request.payload());

        job.setNextFireAt(nextFireCalculator.initialFireTime(job));
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(saved.getId());
        return saved;
    }

    public JobDefinition get(UUID id) {
        return cacheService.get(id).orElseGet(() -> {
            JobDefinition job = findOrThrow(id);
            cacheService.put(job);
            return job;
        });
    }

    public List<JobDefinition> list() {
        return jobDefinitionRepository.findAll();
    }

    @Transactional
    public JobDefinition update(UUID id, UpdateJobRequest request) {
        JobDefinition job = findOrThrow(id);
        if (request.name() != null) job.setName(request.name());
        if (request.priority() != null) job.setPriority(request.priority());
        if (request.maxAttempts() != null) job.setMaxAttempts(request.maxAttempts());
        if (request.baseBackoffMs() != null) job.setBaseBackoffMs(request.baseBackoffMs());
        if (request.maxBackoffMs() != null) job.setMaxBackoffMs(request.maxBackoffMs());
        if (request.payload() != null) job.setPayload(request.payload());
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(id);
        return saved;
    }

    @Transactional
    public JobDefinition pause(UUID id) {
        JobDefinition job = findOrThrow(id);
        job.setStatus(JobStatus.PAUSED);
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(id);
        return saved;
    }

    @Transactional
    public JobDefinition resume(UUID id) {
        JobDefinition job = findOrThrow(id);
        job.setStatus(JobStatus.ACTIVE);
        if (job.getNextFireAt() == null || job.getNextFireAt().isBefore(Instant.now())) {
            job.setNextFireAt(nextFireCalculator.initialFireTime(job));
        }
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(id);
        return saved;
    }

    @Transactional
    public JobDefinition cancel(UUID id) {
        JobDefinition job = findOrThrow(id);
        job.setStatus(JobStatus.CANCELLED);
        job.setNextFireAt(null);
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(id);
        return saved;
    }

    /** Forces an immediate claimable run regardless of the normal schedule. */
    @Transactional
    public JobDefinition runNow(UUID id) {
        JobDefinition job = findOrThrow(id);
        if (job.getStatus() != JobStatus.ACTIVE) {
            job.setStatus(JobStatus.ACTIVE);
        }
        job.setNextFireAt(Instant.now());
        JobDefinition saved = jobDefinitionRepository.save(job);
        cacheService.evict(id);
        return saved;
    }

    private JobDefinition findOrThrow(UUID id) {
        return jobDefinitionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("job not found: " + id));
    }
}
