package com.jobflow.orchestrator.repository;

import com.jobflow.orchestrator.domain.JobDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JobDefinitionRepository extends JpaRepository<JobDefinition, UUID> {

    @Query(value = """
            SELECT id FROM job_definitions
            WHERE status = 'ACTIVE' AND next_fire_at IS NOT NULL AND next_fire_at <= now()
            ORDER BY priority DESC, next_fire_at ASC
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<UUID> lockDueJobIds(@Param("batchSize") int batchSize);
}
