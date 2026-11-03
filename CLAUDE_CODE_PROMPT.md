# CLAUDE_CODE_PROMPT.md

This file exists so that Claude Code (or another AI coding assistant) can
pick this project back up with full context, without re-deriving the
project's intent from scratch. It is not user-facing documentation — see
`README.md` and `docs/` for that.

## What this project is

JobFlow Orchestrator is a distributed job scheduler and workflow engine
built to demonstrate real backend engineering ability for SDE/backend
internship and placement portfolios. It is meant to read as production-
shaped code, not a CRUD tutorial project: concurrent-safe job claiming,
lease-based failure recovery, retry with bounded exponential backoff, and a
validated, dependency-driven workflow DAG engine.

## Non-negotiable constraints for any future change

- **PostgreSQL remains the single source of truth** for scheduling
  correctness. Never make Redis or Kafka load-bearing for what gets
  claimed or executed — see `docs/adr/0003-*.md` and `docs/adr/0004-*.md`.
- **Execution semantics are at-least-once, not exactly-once.** Don't
  "fix" this by adding fake exactly-once guarantees; document tradeoffs
  instead, as `docs/ARCHITECTURE.md` already does.
- **No default task type may execute arbitrary shell commands.** New task
  types implement `TaskHandler` and are registered as Spring beans,
  discovered via `TaskHandlerRegistry` — no other wiring, no shell-exec
  default.
- **Do not invent benchmark numbers, user counts, adoption figures, or
  performance claims.** If you add a claim like "handles 10,000 jobs/sec,"
  it must come from an actual load test committed alongside it, or it
  should not be written at all.
- **Do not copy source from external repositories.** Original
  implementation only.

## Build/verify loop for a future session

```bash
cd backend && mvn verify        # unit + Testcontainers integration tests
cd frontend && npm run build    # production build must succeed
docker compose config -q        # validates docker-compose.yml
```

Also worth running before considering a change complete:
- `grep -rn "TODO\|FIXME"` across `backend/src` and `frontend/src` — none
  should remain in code presented as finished.
- A secrets scan (no committed passwords/keys — `application.yml` should
  only ever read secrets from environment variables with clearly-fake
  local-dev defaults).

## Environment note from the session that built this

The initial build of this project was done in a sandboxed environment
without access to Maven Central, so the backend (Java/Spring) code was
written carefully but could **not** be compiled or test-run in that
session — only manually reviewed. The frontend (npm-based) **was** built
and verified successfully (`vite build` succeeded). If you are an AI
assistant picking this up with real Maven access, running `mvn verify`
as an early step is strongly recommended to catch anything that slipped
through manual review — treat the backend as "written to compile,
not confirmed to compile" until you've run it yourself.

## Where things live

See `README.md` → Project structure, and `CONTRIBUTING.md` for the local
dev loop. `docs/ARCHITECTURE.md` is the most detailed technical reference;
keep it in sync with any behavioral change.
