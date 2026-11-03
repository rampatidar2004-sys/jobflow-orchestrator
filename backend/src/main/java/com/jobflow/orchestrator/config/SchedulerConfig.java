package com.jobflow.orchestrator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(SchedulerProperties.class)
public class SchedulerConfig {

    /**
     * Bounded thread pool that actually runs TaskHandlers. Bounded on purpose:
     * the claim batch size can exceed pool capacity, but execution concurrency
     * never can, so one node can't starve the database or downstream systems.
     */
    @Bean
    public ExecutorService jobWorkerExecutor(SchedulerProperties props) {
        AtomicInteger counter = new AtomicInteger(0);
        ThreadFactory threadFactory = runnable -> {
            Thread t = new Thread(runnable, "jobflow-worker-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return Executors.newFixedThreadPool(props.getWorkerPoolSize(), threadFactory);
    }
}
