# JobFlow Orchestrator

**A distributed job scheduler and dependency-aware workflow engine** built with Java 21, Spring Boot, PostgreSQL, and React.

JobFlow schedules recurring and one-time jobs, coordinates concurrent schedulers, recovers expired executions, retries failures with bounded exponential backoff, and runs multi-step workflows represented as directed acyclic graphs (DAGs).

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.0--M2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-61DAFB?logo=react&logoColor=222)](https://react.dev/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> **Project status:** Portfolio/reference implementation. Job execution semantics are **at-least-once, not exactly-once**. A task that runs longer than its lease can be retried after lease recovery; task handlers that cause external side effects should therefore be idempotent.
>
> **Framework note:** The supplied Maven configuration currently pins Spring Boot `4.0.0-M2` (a milestone build). This is documented as-is rather than silently changing the dependency version. Review and upgrade to a supported stable release before production use.

## What it demonstrates

- **Concurrent scheduling:** Supports `ONCE`, `INTERVAL`, and `CRON` jobs. PostgreSQL `SELECT ... FOR UPDATE SKIP LOCKED` lets multiple scheduler instances claim due jobs without waiting on rows already claimed by another instance.
- **Lease-based recovery:** Each claimed execution receives a time-bounded lease. A recovery process finds expired `CLAIMED`/`RUNNING` executions so work is not left stuck after a worker failure.
- **Bounded retries:** Failed jobs are rescheduled using exponential backoff with a configurable maximum delay and maximum attempt count.
- **Workflow DAGs:** Validates duplicate step keys, unknown dependencies, self-dependencies, and cycles using Kahn's topological-sort algorithm. Steps become eligible as their dependencies succeed.
- **Idempotent workflow starts:** Supports idempotency keys to avoid creating duplicate logical workflow runs.
- **Secured API and operations UI:** JWT-protected REST endpoints are paired with a React operations console for jobs, workflows, and execution status.
- **Observability:** Exposes application metrics through Spring Actuator/Micrometer and Prometheus, with a provisioned Grafana dashboard.
- **Supporting infrastructure:** Uses Redis for short-lived read caching and Kafka for lifecycle notifications; PostgreSQL remains the source of truth for scheduling state.

## Architecture at a glance

```text
              React + Vite Operations Console
                           |
                     JWT-secured REST API
                           |
                  Spring Boot Application
                   /        |         \
             Scheduler   Workflow    Auth/API
                |         Engine        |
                +-----------+-----------+
                            |
              PostgreSQL (source of truth)
                   /                 \
          Redis read cache      Kafka lifecycle events
                            
         Actuator / Micrometer -> Prometheus -> Grafana
```

### Job execution lifecycle

1. Scheduler pollers find due jobs in PostgreSQL.
2. A transaction locks eligible rows using `FOR UPDATE SKIP LOCKED`, creates execution records and leases, and advances the next scheduled fire time.
3. Workers execute the registered task handler.
4. Failed attempts are rescheduled with capped exponential backoff.
5. Expired leases are recovered by a background process.

### Workflow example

```text
prepare
   ├── validate ──┐
   └── enrich ────┴──> finish
```

`finish` is eligible only after both `validate` and `enrich` have succeeded.

## Technology stack

| Area | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 4.0.0-M2, Spring Web, Validation, Data JPA, JDBC, Spring Security |
| Persistence & migrations | PostgreSQL 16, Flyway |
| Cache & messaging | Redis 7, Apache Kafka |
| Authentication | JWT, BCrypt |
| Frontend | React 18, Vite, React Router |
| Observability | Spring Actuator, Micrometer, Prometheus, Grafana |
| Testing | JUnit 5, Mockito, Spring test support, Testcontainers, Awaitility |
| Build & infrastructure | Maven, Docker, Docker Compose, GitHub Actions |

## Run locally with Docker Compose

### Prerequisites

- Docker Desktop with Docker Compose
- Git

### Start the stack

```bash
git clone https://github.com/rampatidar2004-sys/jobflow-orchestrator.git
cd jobflow-orchestrator

# Optional: create a local environment file and replace the development-only JWT secret.
cp .env.example .env
# Edit .env before using the stack beyond a local demo.

docker compose up --build
```

Local services:

| Service | URL | Local demo credentials |
|---|---|---|
| React console | http://localhost:5173 | Register a new user in the UI |
| REST API | http://localhost:8080/api | JWT required for protected routes |
| Health endpoint | http://localhost:8080/actuator/health | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3000 | `admin` / `admin` (local demo only) |

**Security:** The Compose file contains development-only defaults for local containers. Do not expose this stack to the internet or reuse these passwords/secrets in a deployed environment. Set a strong random `JWT_SECRET` and secure all service credentials before deployment.

Stop the stack:

```bash
docker compose down
```

Remove local database and Grafana data volumes (destructive):

```bash
docker compose down -v
```

## API example

Register a local demo account and create a one-time job:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo-user","password":"demo-password-123"}' | jq -r .token)

curl -s -X POST http://localhost:8080/api/jobs \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "demo-once",
    "taskType": "demo",
    "scheduleType": "ONCE",
    "priority": 5,
    "maxAttempts": 3,
    "payload": "{\"durationMs\":200}"
  }'
```

See [`docs/API.md`](docs/API.md) for endpoint details and [`postman/`](postman/) for the Postman collection.

## Run checks

```bash
# Backend unit and integration tests (integration tests use Testcontainers)
cd backend
mvn verify
cd ..

# Frontend production build
cd frontend
npm ci
npm run build
cd ..

# Validate Docker Compose configuration
docker compose config -q
```

Or run the repository verification helper:

```bash
./scripts/verify.sh
```

## Repository layout

```text
backend/       Spring Boot API, scheduler, job workers, workflow engine
frontend/      React + Vite operations console
infra/         Prometheus and Grafana provisioning
postman/       API request collection
docs/          Architecture, API reference, ADRs, system-design notes
scripts/       Local development and verification helpers
```

## Design notes

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md): claiming, lease recovery, retry behavior, workflow execution, and delivery semantics.
- [`docs/SYSTEM_DESIGN_INTERVIEW.md`](docs/SYSTEM_DESIGN_INTERVIEW.md): architectural decisions and trade-offs in interview format.
- [`docs/API.md`](docs/API.md): REST API reference.
- [`docs/adr/`](docs/adr/): architecture decision records.
- [`SECURITY.md`](SECURITY.md): reporting security issues and handling secrets.

## Author

**Shriram Patidar** — MCA, NIT Tiruchirappalli  
[GitHub profile](https://github.com/rampatidar2004-sys)

## License

MIT — see [`LICENSE`](LICENSE).
