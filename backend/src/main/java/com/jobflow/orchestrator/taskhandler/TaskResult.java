package com.jobflow.orchestrator.taskhandler;

/**
 * Outcome of a single task execution attempt.
 */
public record TaskResult(boolean success, String summary) {

    public static TaskResult success(String summary) {
        return new TaskResult(true, summary);
    }

    public static TaskResult failure(String summary) {
        return new TaskResult(false, summary);
    }
}
