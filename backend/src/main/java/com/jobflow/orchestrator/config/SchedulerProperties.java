package com.jobflow.orchestrator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobflow.scheduler")
public class SchedulerProperties {

    /** Size of the bounded worker thread pool that actually runs TaskHandlers. */
    private int workerPoolSize = 8;

    /** Max jobs claimed per poll cycle. */
    private int claimBatchSize = 20;

    /** How often the scheduler polls for due jobs. */
    private long pollIntervalMs = 1000;

    /** Lease duration granted to a claimed execution before it is considered expired. */
    private long leaseDurationSeconds = 60;

    /** How often the lease-recovery sweep runs. */
    private long leaseRecoveryIntervalMs = 15000;

    public int getWorkerPoolSize() { return workerPoolSize; }
    public void setWorkerPoolSize(int workerPoolSize) { this.workerPoolSize = workerPoolSize; }

    public int getClaimBatchSize() { return claimBatchSize; }
    public void setClaimBatchSize(int claimBatchSize) { this.claimBatchSize = claimBatchSize; }

    public long getPollIntervalMs() { return pollIntervalMs; }
    public void setPollIntervalMs(long pollIntervalMs) { this.pollIntervalMs = pollIntervalMs; }

    public long getLeaseDurationSeconds() { return leaseDurationSeconds; }
    public void setLeaseDurationSeconds(long leaseDurationSeconds) { this.leaseDurationSeconds = leaseDurationSeconds; }

    public long getLeaseRecoveryIntervalMs() { return leaseRecoveryIntervalMs; }
    public void setLeaseRecoveryIntervalMs(long leaseRecoveryIntervalMs) { this.leaseRecoveryIntervalMs = leaseRecoveryIntervalMs; }
}
