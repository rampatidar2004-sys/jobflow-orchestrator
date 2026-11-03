# ADR 0003: Redis is a cache only, never authoritative

## Status
Accepted

## Context
Redis is fast and convenient for caching job-detail reads, but using it for
anything that affects scheduling correctness would introduce a second
system that must be consistent with PostgreSQL for the system to behave
correctly.

## Decision
Redis caches `GET /jobs/{id}` reads for 30 seconds. Every write path
(create, update, pause/resume/cancel, claim, execution outcome) evicts the
corresponding key. Reads fall back to PostgreSQL transparently on any Redis
error.

## Consequences
- Redis being down degrades read latency, never correctness.
- The cache can be briefly stale (up to the TTL) after a write from a
  different process, but never durably wrong, since PostgreSQL is always
  read on a cache miss.
