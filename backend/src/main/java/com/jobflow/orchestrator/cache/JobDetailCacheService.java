package com.jobflow.orchestrator.cache;

import com.jobflow.orchestrator.domain.JobDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-lived Redis cache for job detail reads. Purely a cache: Redis is
 * never the source of truth and every write path (claim, create, update,
 * pause/resume/cancel) evicts the corresponding key so the cache can only
 * ever be briefly stale, never durably wrong. If Redis is unavailable, reads
 * fall back to PostgreSQL transparently.
 */
@Service
public class JobDetailCacheService {

    private static final Logger log = LoggerFactory.getLogger(JobDetailCacheService.class);
    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String KEY_PREFIX = "job-detail:";

    private final RedisTemplate<String, JobDefinition> redisTemplate;

    public JobDetailCacheService(RedisTemplate<String, JobDefinition> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<JobDefinition> get(UUID jobId) {
        try {
            JobDefinition cached = redisTemplate.opsForValue().get(KEY_PREFIX + jobId);
            return Optional.ofNullable(cached);
        } catch (Exception e) {
            log.debug("redis cache read failed, falling back to database: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void put(JobDefinition job) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + job.getId(), job, TTL);
        } catch (Exception e) {
            log.debug("redis cache write failed, continuing without cache: {}", e.getMessage());
        }
    }

    public void evict(UUID jobId) {
        try {
            redisTemplate.delete(KEY_PREFIX + jobId);
        } catch (Exception e) {
            log.debug("redis cache evict failed: {}", e.getMessage());
        }
    }
}
