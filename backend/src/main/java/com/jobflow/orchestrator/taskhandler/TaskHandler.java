package com.jobflow.orchestrator.taskhandler;

/**
 * Contract for a unit of work the scheduler can execute.
 *
 * New task types are added by implementing this interface and registering the
 * implementation as a Spring bean — {@link TaskHandlerRegistry} discovers every
 * bean of this type via dependency injection, keyed by {@link #taskType()}.
 * There is no built-in handler that executes arbitrary shell commands; adding
 * one is a deliberate choice a project operator would have to make explicitly.
 */
public interface TaskHandler {

    /**
     * Unique identifier stored on {@code job_definitions.task_type}. Must be
     * stable — changing it orphans existing job definitions.
     */
    String taskType();

    /**
     * Execute the task. Implementations should throw {@link TaskExecutionException}
     * (or let a runtime exception propagate) on failure; the scheduler treats any
     * thrown exception as a failed attempt eligible for retry.
     *
     * @param context execution context: job id, attempt number, and payload
     */
    TaskResult execute(TaskExecutionContext context) throws Exception;
}
