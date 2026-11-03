-- JobFlow Orchestrator: core schema
-- PostgreSQL is the source of truth for all durable scheduling state.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'OPERATOR',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE job_definitions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name              VARCHAR(200) NOT NULL,
    task_type         VARCHAR(100) NOT NULL,
    schedule_type     VARCHAR(20)  NOT NULL CHECK (schedule_type IN ('ONCE', 'INTERVAL', 'CRON')),
    cron_expression   VARCHAR(120),
    interval_seconds  BIGINT,
    run_at            TIMESTAMPTZ,
    priority          INTEGER      NOT NULL DEFAULT 5,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                        CHECK (status IN ('ACTIVE', 'PAUSED', 'CANCELLED')),
    max_attempts      INTEGER      NOT NULL DEFAULT 5,
    base_backoff_ms   BIGINT       NOT NULL DEFAULT 1000,
    max_backoff_ms    BIGINT       NOT NULL DEFAULT 300000,
    payload           JSONB        NOT NULL DEFAULT '{}'::jsonb,
    next_fire_at      TIMESTAMPTZ,
    created_by        UUID REFERENCES users(id),
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_definitions_claimable
    ON job_definitions (next_fire_at, priority DESC)
    WHERE status = 'ACTIVE';

CREATE TABLE job_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id            UUID NOT NULL REFERENCES job_definitions(id) ON DELETE CASCADE,
    attempt_number    INTEGER NOT NULL DEFAULT 1,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','CLAIMED','RUNNING','SUCCEEDED','FAILED','RETRY_SCHEDULED','CANCELLED')),
    worker_id         VARCHAR(100),
    lease_expires_at  TIMESTAMPTZ,
    started_at        TIMESTAMPTZ,
    finished_at       TIMESTAMPTZ,
    error_message     TEXT,
    result_summary    TEXT,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_job_executions_job_id ON job_executions (job_id);
CREATE INDEX idx_job_executions_lease ON job_executions (status, lease_expires_at);

CREATE TABLE workflow_definitions (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name             VARCHAR(200) NOT NULL,
    created_by       UUID REFERENCES users(id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workflow_steps (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id      UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    step_key         VARCHAR(100) NOT NULL,
    task_type        VARCHAR(100) NOT NULL,
    payload          JSONB NOT NULL DEFAULT '{}'::jsonb,
    UNIQUE (workflow_id, step_key)
);

CREATE TABLE workflow_step_dependencies (
    workflow_step_id UUID NOT NULL REFERENCES workflow_steps(id) ON DELETE CASCADE,
    depends_on_key    VARCHAR(100) NOT NULL,
    PRIMARY KEY (workflow_step_id, depends_on_key)
);

CREATE TABLE workflow_executions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id       UUID NOT NULL REFERENCES workflow_definitions(id) ON DELETE CASCADE,
    idempotency_key   VARCHAR(150),
    status            VARCHAR(20) NOT NULL DEFAULT 'RUNNING'
                        CHECK (status IN ('RUNNING','SUCCEEDED','FAILED','CANCELLED')),
    started_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at       TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_workflow_exec_idempotency
    ON workflow_executions (workflow_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE workflow_step_executions (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_execution_id UUID NOT NULL REFERENCES workflow_executions(id) ON DELETE CASCADE,
    step_key              VARCHAR(100) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','READY','RUNNING','SUCCEEDED','FAILED','SKIPPED')),
    job_execution_id      UUID REFERENCES job_executions(id),
    started_at            TIMESTAMPTZ,
    finished_at           TIMESTAMPTZ,
    UNIQUE (workflow_execution_id, step_key)
);

CREATE INDEX idx_wf_step_exec_status ON workflow_step_executions (workflow_execution_id, status);
