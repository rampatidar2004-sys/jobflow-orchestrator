package com.jobflow.orchestrator.scheduler;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Stable identifier for this scheduler process, used to tag claimed
 * executions ({@code job_executions.worker_id}) so leases and history are
 * attributable to a specific worker instance in a distributed fleet.
 */
@Component
public class WorkerIdentity {

    private final String workerId;

    public WorkerIdentity() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown-host";
        }
        this.workerId = host + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String id() {
        return workerId;
    }
}
