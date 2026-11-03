# Architecture

## Overview

JobFlow Orchestrator has two closely related engines built on the same
primitives: a **job scheduler** (single tasks on a schedule) and a
**workflow engine** (a DAG of steps). Both run their actual work through the
same bounded thread pool and the same `TaskHandler` abstraction; the
workflow engine's step scheduling is dependency-driven rather than
time-driven.

PostgreSQL is the single source of truth for both. Redis and Kafka are
supporting infrastructure — a cache and a notification channel,
respectively — and neither is ever consulted to decide what gets executed.

## Job claiming

Scheduler instances poll on a fixed interval (`jobflow.scheduler.poll-interval-ms`).
Each poll:

1. Runs `SELECT id FROM job_definitions WHERE status = 'ACTIVE' AND next_fire_at <= now() ORDER BY priority DESC, next_fire_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED`.
2. For each locked row, creates a `job_executions` row with status `CLAIMED`,
   a `worker_id`, and a `lease_expires_at` timestamp.
3. Advances the job's `next_fire_at` (or clears it, for `ONCE` jobs) in the
   same transaction as the claim.

`SKIP LOCKED` is what makes this safe with multiple scheduler instances
running concurrently: a row already locked by another instance's
transaction is simply skipped rather than blocked on, so two instances
never claim the same job.

## Leases and recovery

Every claimed execution has a lease (`jobflow.scheduler.lease-duration-seconds`).
A background sweep (`LeaseRecoveryService`, on its own interval) locks
executions whose lease expired while still `CLAIMED` or `RUNNING` — using
the same `FOR UPDATE SKIP LOCKED` pattern — marks them `FAILED`, and
reschedules the parent job immediately if attempts remain.

This is what lets the system tolerate a worker crashing mid-execution: no
external coordinator is needed to notice the failure, because the lease
simply expires.

## Retry and backoff

On failure, if `attempt_number < max_attempts`, the next attempt is
scheduled at `now() + min(base_backoff_ms * 2^(attempt-1), max_backoff_ms)`.
Both the base and the ceiling are configurable per job.

## Execution semantics: at-least-once

**This system provides at-least-once execution, not exactly-once.** Two
places where a task can run more than once for a single logical trigger:

- A worker completes a task successfully but crashes (or the network
  partitions) before its `SUCCEEDED` write commits. The lease later expires
  and a different worker retries — the task runs twice.
- A worker is merely slow (e.g. a long GC pause) rather than actually dead.
  If it exceeds its lease before finishing, another worker may claim the
  retry while the original is still running.

`TaskHandler` implementations that have external side effects (writing to
another system, sending a notification) should be written to be idempotent
if that matters for the use case — e.g. keyed by `executionId`.

## Workflow engine

A workflow is a set of steps, each with a `stepKey`, a `taskType`, and a set
of `dependsOn` keys. Validation at creation time (`WorkflowDagValidator`):

1. **Duplicate-step validation** — no two steps share a `stepKey`.
2. **Unknown-dependency validation** — every `dependsOn` entry must name a
   step that exists in the same workflow.
3. **Cycle detection** — Kahn's algorithm: repeatedly remove zero-indegree
   nodes; if any nodes remain once no more can be removed, the graph has a
   cycle and the workflow is rejected.

At execution time (`WorkflowProgressService`), each step starts `PENDING`.
A step becomes `READY` (and is dispatched to the worker pool) once every
dependency has `SUCCEEDED`. A step becomes `SKIPPED` if any dependency
`FAILED` or was itself `SKIPPED` — this cascades transitively. The workflow
execution is finalized `SUCCEEDED` or `FAILED` once no step is left
`PENDING`/`READY`/`RUNNING`.

Progress advancement locks the parent `workflow_executions` row
(`FOR UPDATE`) first, so concurrent completions of sibling steps — each
running on a different worker thread — serialize instead of racing.

### Idempotency keys

Starting an execution with an `idempotencyKey` that already has a row for
that `(workflow_id, idempotency_key)` pair (enforced by a unique partial
index) returns the existing execution instead of starting a duplicate. A
unique-constraint violation on the race between two concurrent callers is
caught and resolved the same way, rather than surfaced as an error.

## Caching

`JobDetailCacheService` caches individual job-detail reads in Redis for 30
seconds. Every write path (create, update, pause/resume/cancel, claim)
evicts the corresponding key. If Redis is unreachable, reads fall back to
PostgreSQL transparently — Redis being down never breaks scheduling
correctness, only removes a read optimization.

## Messaging

`JobLifecycleEventPublisher` publishes `COMPLETED` / `RETRY` / `FAILED`
events to the `job-lifecycle-events` Kafka topic after the corresponding
state is already committed to PostgreSQL. A publish failure is logged and
swallowed — it never rolls back or blocks the underlying scheduling
decision.

## Extensibility

New task types implement `TaskHandler` and register as a Spring bean;
`TaskHandlerRegistry` discovers every such bean via constructor injection
and indexes it by `taskType()`. There is no built-in handler that executes
arbitrary shell commands — adding shell execution as a task type is a
deliberate choice a project operator would make explicitly, not a default.
