# ADR 0001: Use PostgreSQL `FOR UPDATE SKIP LOCKED` for job claiming

## Status
Accepted

## Context
Multiple scheduler instances need to claim due jobs without ever claiming
the same job twice, and without introducing a separate coordination service
(leader election, external queue) as an additional system that must stay
correct and available.

## Decision
Use `SELECT ... FOR UPDATE SKIP LOCKED` against `job_definitions`, inside a
transaction that also creates the `job_executions` row and advances
`next_fire_at`, so the claim and the schedule advance are atomic.

## Consequences
- No extra infrastructure beyond PostgreSQL is required for correct,
  concurrent claiming.
- Claim throughput is bounded by PostgreSQL write throughput; acceptable at
  the scale this project targets (see docs/SYSTEM_DESIGN_INTERVIEW.md
  §7 for what changes at larger scale).
- PostgreSQL is now unambiguously the single source of truth, which
  simplifies reasoning about every other component (Redis, Kafka) as
  non-load-bearing.
