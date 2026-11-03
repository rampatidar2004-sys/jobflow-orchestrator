# JobFlow Orchestrator as a System Design Interview Answer

This document reframes the project as an answer to "design a distributed job
scheduler," the way you'd walk an interviewer through it on a whiteboard.

## 1. Requirements

**Functional**
- Schedule one-off, interval, and cron-based jobs.
- Run jobs with bounded concurrency across multiple worker processes.
- Retry failed jobs with backoff, up to a max attempt count.
- Support multi-step workflows with dependencies between steps.
- Expose a REST API and an operator-facing console.

**Non-functional**
- Survive a worker crashing mid-job without losing or duplicating work
  beyond an explicit, documented at-least-once guarantee.
- Scale horizontally: multiple scheduler/worker instances, no single
  coordinator that has to stay up.
- Observable: metrics, dashboards, structured execution history.

## 2. Core design decision: who owns "what runs next"?

Three common approaches, and why this project picked the first:

1. **The database is the queue**, claimed via `SELECT ... FOR UPDATE SKIP LOCKED`.
   No separate coordination service, no leader election, no extra moving
   part to run and monitor. Throughput is bounded by the database, which
   is the right tradeoff at the scale a single-team scheduler operates at.
2. **A dedicated queue (SQS/RabbitMQ) in front of the database.** More
   moving parts, better raw queue throughput, and it decouples claiming
   from the row that holds durable state — which then needs to stay in
   sync with the queue.
3. **Leader-elected single scheduler.** Simpler claiming logic (no
   contention), but the leader is a single point of failure for *dispatch*,
   even if workers themselves are still redundant.

`SKIP LOCKED` was chosen because it keeps PostgreSQL as the only stateful
system in the claiming path, and it's a well-understood, battle-tested
primitive (this is close to how several real-world queue-on-Postgres
libraries work).

## 3. Handling failure: leases instead of heartbeats

Rather than requiring workers to heartbeat a coordinator, each claimed
execution gets a **time-boxed lease**. A background sweep reclaims any
execution whose lease expired without finishing. This means:

- No coordinator needs to track worker liveness.
- A worker that's merely slow (long GC pause) risks a duplicate execution
  if it out-lives its lease — an explicit, documented at-least-once
  tradeoff rather than a hidden bug.
- Lease duration is a tunable: shorter leases recover faster but risk more
  false-positive duplicate executions for genuinely slow (not dead) tasks.

## 4. Handling retries: exponential backoff with a ceiling

Unbounded exponential backoff can push a retry arbitrarily far into the
future after enough failures; a ceiling (`max_backoff_ms`) keeps retry
latency bounded while still backing off aggressively early on, which is
the usual shape you want: don't hammer a struggling dependency, but don't
let a job go silent for hours either.

## 5. Workflow DAGs: validation vs. runtime concerns

Cycle detection is checked once, at creation time, with Kahn's algorithm —
O(V+E), and it also yields a valid topological order for display. Pushing
this to creation time (rather than discovering a cycle at execution time)
means a broken workflow definition fails fast with a clear error instead of
silently deadlocking mid-run.

At runtime, dependency resolution is push-based: each step completion
triggers a re-evaluation of the DAG (`WorkflowProgressService.advance`)
rather than a poller repeatedly scanning for ready steps. This keeps
workflow step latency low (steps start as soon as their dependencies clear,
not on the next poll tick) without adding a second scheduling loop.

## 6. Why Redis and Kafka are optional, not load-bearing

A common design smell in take-home systems is treating a cache or a
message bus as part of the correctness story. Here:

- **Redis** only ever caches read responses; the eviction happens on every
  write path, and a Redis outage degrades to "always hit Postgres," not to
  incorrect scheduling.
- **Kafka** only ever carries after-the-fact notifications; a publish
  failure is logged and swallowed rather than propagated, because the
  scheduling decision it's describing is already durable by the time the
  event is published.

This is a deliberate choice: it keeps the number of systems that must be
correct for scheduling to be correct to exactly one — PostgreSQL.

## 7. What would change at 10x/100x scale

- **Claim batch size and poll interval** become the main tuning knobs
  before the `job_definitions` table itself needs partitioning by, e.g.,
  tenant or time bucket.
- **Execution history** (`job_executions`) grows unbounded; a real
  deployment would need a retention/archival job, which this project
  intentionally leaves out as a documented gap rather than faking it.
- **Kafka** would become load-bearing for downstream consumers reacting to
  job outcomes, which would then require at-least-once consumer-side
  idempotency on the *reading* end too.
