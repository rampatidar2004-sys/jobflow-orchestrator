package com.jobflow.orchestrator.support;

import com.jobflow.orchestrator.cache.JobDetailCacheService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests that need a real PostgreSQL and Kafka
 * (via Testcontainers) to exercise the SKIP LOCKED claim path, lease
 * recovery, and Kafka lifecycle event publication end to end.
 *
 * Redis is intentionally NOT a Testcontainers dependency here:
 * {@link JobDetailCacheService} is mocked to always miss, which exercises
 * the documented cache-fallback-to-Postgres path directly rather than
 * standing up a fourth container just to prove the cache is non-load-bearing.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("jobflow")
                    .withUsername("jobflow")
                    .withPassword("jobflow");

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        // Point at an address nothing is listening on so every cache call fails fast
        // and JobDetailCacheService's fallback-to-Postgres path is exercised for real.
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> "1");
    }
}
