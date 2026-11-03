# Contributing to JobFlow Orchestrator

Thanks for considering a contribution. This is a portfolio-grade reference
project, but it's built to real engineering standards and welcomes real
improvements.

## Getting set up

**Prerequisites:** Java 21, Maven, Node 20+, Docker & Docker Compose.

```bash
# Full stack (Postgres, Redis, Kafka, backend, frontend, Prometheus, Grafana)
docker compose up --build

# Backend only, against your own Postgres/Redis/Kafka
cd backend
mvn spring-boot:run

# Frontend only (proxies /api to localhost:8080 by default)
cd frontend
npm install
npm run dev
```

## Project layout

```text
backend/    Spring Boot service — scheduler, workflow engine, REST API
frontend/   React + Vite operations console
infra/      Prometheus + Grafana provisioning
docs/       Architecture notes, API reference, ADRs
postman/    Example API requests
scripts/    Local dev helper scripts
```

## Making a change

1. Create a branch off `main`.
2. Write the change, plus tests: unit tests for pure logic (backoff math,
   DAG validation, cron computation), Testcontainers integration tests for
   anything touching Postgres/Kafka.
3. Run the full check locally before opening a PR:
   ```bash
   cd backend && mvn verify
   cd frontend && npm run build
   docker compose config -q   # validates the compose file
   ```
4. Update `docs/ARCHITECTURE.md` or `docs/API.md` if you changed behavior
   those documents describe.
5. Open a PR using the provided template.

## Adding a new task type

Implement `TaskHandler`, register it as a Spring bean, and it is picked up
automatically by `TaskHandlerRegistry` — no other wiring needed. Do not add
a handler that shells out to arbitrary commands; see `SECURITY.md` for why.

## Code style

- Backend: standard Java conventions, constructor injection, no field
  injection, prefer immutable DTOs (`record`) for API payloads.
- Frontend: functional components + hooks, no class components, keep API
  calls in `src/api/client.js` rather than scattered `fetch` calls.

## Commit messages

Short imperative subject line (`Add lease recovery sweep`, not `Added...`),
with a body explaining *why* when the change isn't self-evident.
