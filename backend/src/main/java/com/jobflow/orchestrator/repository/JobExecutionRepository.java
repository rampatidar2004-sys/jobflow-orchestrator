package com.jobflow.orchestrator.repository;

import com.jobflow.orchestrator.domain.ExecutionStatus;
import com.jobflow.orchestrator.domain.JobExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobExecutionRepository extends JpaRepository<JobExecution, UUID> {

    List<JobExecution> findByJobIdOrderByCreatedAtDesc(UUID jobId);

    @Query(value = """
            SELECT id FROM job_executions
            WHERE status IN ('CLAIMED', 'RUNNING') AND lease_expires_at < now()
            ORDER BY lease_expires_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockExpiredLeaseIds(@Param("batchSize") int batchSize);

    long countByJobIdAndStatus(UUID jobId, ExecutionStatus status);
}
