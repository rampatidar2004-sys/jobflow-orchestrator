export default function ArchitecturePage() {
  return (
    <div>
      <div className="topbar"><h1>Architecture</h1></div>

      <div className="arch-section">
        <h2>Scheduling core</h2>
        <p>
          PostgreSQL is the single source of truth. Scheduler instances poll for due jobs and claim them with
          <code> FOR UPDATE SKIP LOCKED</code>, so multiple instances never claim the same job twice. Each claim
          creates a job execution row with a worker id and a time-boxed lease; a background sweep recovers
          executions whose lease expired without completing (a crashed or partitioned worker) and makes them
          claimable again.
        </p>
      </div>

      <div className="arch-section">
        <h2>Execution semantics</h2>
        <p>
          Execution is <strong>at-least-once, not exactly-once</strong>. Lease recovery can, in rare timing
          windows, result in a task running more than once if a worker was merely slow rather than actually dead.
          Task handlers that need exactly-once side effects should be written to be idempotent.
        </p>
      </div>

      <div className="arch-section">
        <h2>Workflow DAGs</h2>
        <p>
          Workflows are validated at creation time: duplicate step keys and references to unknown steps are
          rejected, and the dependency graph is checked for cycles using Kahn's algorithm. At execution time,
          steps become eligible to run only once every dependency has succeeded; a failed or skipped dependency
          propagates as a skip to its dependents.
        </p>
      </div>

      <div className="arch-section">
        <h2>Caching and messaging</h2>
        <p>
          Redis holds a short-lived cache of job detail reads only — it is never consulted to decide what gets
          claimed or executed, and every write path evicts the relevant key. Kafka carries lifecycle
          notifications (completed / retry / failed) for downstream consumers; a publish failure never blocks or
          rolls back the underlying scheduling decision, which is already durable in Postgres by that point.
        </p>
      </div>

      <div className="arch-section">
        <h2>Extensibility</h2>
        <p>
          New task types implement the <code>TaskHandler</code> interface and are registered as Spring beans —
          the <code>TaskHandlerRegistry</code> discovers them automatically via dependency injection. There is no
          built-in handler that executes arbitrary shell commands.
        </p>
      </div>
    </div>
  );
}
