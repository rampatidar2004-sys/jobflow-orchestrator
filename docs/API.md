# API Reference

Base URL: `http://localhost:8080/api`

All endpoints except `/auth/**` and the public Actuator endpoints require a
`Authorization: Bearer <token>` header.

## Auth

### `POST /auth/register`

```json
{ "username": "demo", "password": "demo1234" }
```
→ `201 Created`, `{ "token": "...", "tokenType": "Bearer" }`

### `POST /auth/login`

```json
{ "username": "demo", "password": "demo1234" }
```
→ `200 OK`, `{ "token": "...", "tokenType": "Bearer" }`. `401` on bad credentials.

## Jobs

### `POST /jobs`

```json
{
  "name": "nightly-report",
  "taskType": "demo",
  "scheduleType": "CRON",
  "cronExpression": "0 2 * * *",
  "priority": 5,
  "maxAttempts": 5,
  "baseBackoffMs": 1000,
  "maxBackoffMs": 300000,
  "payload": "{\"durationMs\":500}"
}
```

`scheduleType` is one of `ONCE`, `INTERVAL`, `CRON`.
- `ONCE` → optionally set `runAt` (ISO-8601 instant); defaults to now.
- `INTERVAL` → set `intervalSeconds`.
- `CRON` → set `cronExpression` (Unix 5-field cron).

→ `201 Created` with the job resource.

### `GET /jobs` — list all jobs.

### `GET /jobs/{id}` — get one job (served from Redis cache when warm).

### `PUT /jobs/{id}` — update mutable fields (`name`, `priority`, `maxAttempts`, `baseBackoffMs`, `maxBackoffMs`, `payload`). Schedule type/expression are immutable after creation — create a new job instead.

### `POST /jobs/{id}/pause` — stop future claims; in-flight executions finish normally.

### `POST /jobs/{id}/resume` — resume an `ACTIVE` schedule.

### `POST /jobs/{id}/run` — force an immediate claimable run, outside the normal schedule.

### `POST /jobs/{id}/cancel` — stop the job permanently (clears `next_fire_at`).

### `GET /jobs/{id}/executions` — execution history, most recent first.

## Workflows

### `POST /workflows`

```json
{
  "name": "prepare-validate-enrich-finish",
  "steps": [
    { "stepKey": "prepare", "taskType": "demo", "payload": "{}", "dependsOn": [] },
    { "stepKey": "validate", "taskType": "demo", "payload": "{}", "dependsOn": ["prepare"] },
    { "stepKey": "enrich",   "taskType": "demo", "payload": "{}", "dependsOn": ["prepare"] },
    { "stepKey": "finish",   "taskType": "demo", "payload": "{}", "dependsOn": ["validate", "enrich"] }
  ]
}
```

Rejected with `400` if any step key repeats, any `dependsOn` references an
unknown step, or the graph has a cycle.

### `GET /workflows` — list workflow definitions.

### `GET /workflows/{id}` — get one workflow, including its steps.

### `POST /workflows/{id}/executions`

```json
{ "idempotencyKey": "2026-09-23-nightly" }
```

`idempotencyKey` is optional. If a prior execution with the same key
exists for this workflow, it is returned unchanged instead of starting a
new run. → `201 Created` with the execution resource.

### `GET /workflows/{id}/executions` — executions for a workflow, most recent first.

### `GET /workflows/executions/{executionId}` — get one execution.

### `GET /workflows/executions/{executionId}/steps` — per-step status for an execution.

## Errors

Errors are returned as:

```json
{
  "timestamp": "2026-09-23T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "step 'finish' depends on unknown step 'publish'"
}
```

| Status | Meaning |
|---|---|
| 400 | Validation failure (bad request body, invalid DAG) |
| 401 | Missing/invalid/expired token |
| 404 | Resource not found |
| 409 | Username already taken (register) |

## Observability

- `GET /actuator/health` — liveness/readiness (public).
- `GET /actuator/prometheus` — Prometheus scrape endpoint (public).
- `GET /actuator/metrics` — Micrometer metric names (authenticated).
