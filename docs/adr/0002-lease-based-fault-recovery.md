# ADR 0002: Lease-based fault recovery instead of heartbeats

## Status
Accepted

## Context
Workers can crash, be killed, or lose network connectivity mid-execution.
The system needs to recover that work without a central coordinator
tracking worker liveness.

## Decision
Every claimed execution gets a time-boxed lease (`lease_expires_at`). A
background sweep (`LeaseRecoveryService`) reclaims any execution whose
lease expired while still `CLAIMED`/`RUNNING`, marks it `FAILED`, and
reschedules the parent job if attempts remain.

## Consequences
- No heartbeat protocol or liveness coordinator is needed.
- This is explicitly an at-least-once guarantee: a worker that is merely
  slow (not dead) past its lease can have its work duplicated by a second
  worker. This is documented, not hidden — see docs/ARCHITECTURE.md.
- Lease duration is a single tunable that trades recovery latency against
  false-positive duplicate execution risk.
