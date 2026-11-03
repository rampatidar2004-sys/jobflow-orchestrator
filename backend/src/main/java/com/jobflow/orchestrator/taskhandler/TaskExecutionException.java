package com.jobflow.orchestrator.taskhandler;

/** Signals that a task attempt failed in a way the scheduler should treat as retryable. */
public class TaskExecutionException extends RuntimeException {
    public TaskExecutionException(String message) {
        super(message);
    }

    public TaskExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
