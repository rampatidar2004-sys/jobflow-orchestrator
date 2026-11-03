# ADR 0005: Validate workflow DAGs at creation time, not execution time

## Status
Accepted

## Context
A workflow with duplicate step keys, dangling dependencies, or a dependency
cycle can't be executed correctly. Discovering that at execution time means
a broken definition can be created and only fails later, potentially
mid-run.

## Decision
`WorkflowDagValidator` checks duplicate step keys, unknown dependency
references, and cycles (via Kahn's algorithm) synchronously in
`WorkflowService.create()`, before the workflow is ever persisted.

## Consequences
- Invalid workflows are rejected immediately with a specific error message,
  not discovered as a stuck execution later.
- Kahn's algorithm also yields a valid topological order as a side effect,
  reused for validation but not required for runtime scheduling, which is
  dependency-driven rather than order-driven.
