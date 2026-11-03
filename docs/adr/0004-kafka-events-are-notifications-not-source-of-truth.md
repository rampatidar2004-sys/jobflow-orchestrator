# ADR 0004: Kafka carries notifications, not scheduling state

## Status
Accepted

## Context
Downstream systems may want to react to job completion, retry, and failure
events. Using Kafka as part of the scheduling decision path (e.g. having
workers consume from Kafka to know what to run) would make Kafka
availability a correctness dependency for scheduling itself.

## Decision
`JobLifecycleEventPublisher` publishes `COMPLETED`/`RETRY`/`FAILED` events
to Kafka only after the corresponding state change is already committed to
PostgreSQL. A publish failure is logged and swallowed rather than
propagated or retried at the scheduling layer.

## Consequences
- Kafka being down never blocks or corrupts scheduling.
- Downstream consumers of `job-lifecycle-events` must tolerate at-least-once
  delivery and, in rare cases, a missed event if Kafka was down at publish
  time — a real gap, documented rather than hidden. A production system
  needing stronger delivery guarantees would add an outbox pattern here.
