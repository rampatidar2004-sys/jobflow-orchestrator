import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../api/client';
import StateMessage from '../components/StateMessage';
import StatusBadge from '../components/StatusBadge';
import DagView from '../components/DagView';

export default function WorkflowDetailPage() {
  const { id } = useParams();
  const [workflow, setWorkflow] = useState(null);
  const [executions, setExecutions] = useState([]);
  const [selectedExecution, setSelectedExecution] = useState(null);
  const [stepStatuses, setStepStatuses] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [starting, setStarting] = useState(false);

  const load = useCallback(async () => {
    try {
      const [w, execs] = await Promise.all([api.getWorkflow(id), api.workflowExecutions(id)]);
      setWorkflow(w);
      setExecutions(execs);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!selectedExecution) return;
    const refresh = async () => {
      const steps = await api.workflowExecutionSteps(selectedExecution);
      setStepStatuses(Object.fromEntries(steps.map((s) => [s.stepKey, s.status])));
    };
    refresh();
    const interval = setInterval(refresh, 3000);
    return () => clearInterval(interval);
  }, [selectedExecution]);

  async function start() {
    setStarting(true);
    try {
      const exec = await api.startWorkflow(id);
      await load();
      setSelectedExecution(exec.id);
    } catch (err) {
      setError(err.message);
    } finally {
      setStarting(false);
    }
  }

  return (
    <div>
      <div className="topbar">
        <div>
          <Link to="/workflows" style={{ fontSize: 12 }}>&larr; Workflows</Link>
          <h1 style={{ marginTop: 4 }}>{workflow?.name || '…'}</h1>
        </div>
        <button className="btn" onClick={start} disabled={starting || loading}>
          {starting ? 'Starting…' : 'Start execution'}
        </button>
      </div>

      <StateMessage loading={loading} error={error} />

      {workflow && (
        <>
          <DagView steps={workflow.steps} statusByStepKey={selectedExecution ? stepStatuses : null} />

          <h2 style={{ fontSize: 14, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em', marginTop: 24 }}>
            Executions
          </h2>
          {executions.length === 0 ? (
            <div className="state-message">No executions yet — start one above.</div>
          ) : (
            <table>
              <thead><tr><th>Started</th><th>Status</th><th>Idempotency key</th><th></th></tr></thead>
              <tbody>
                {executions.map((e) => (
                  <tr key={e.id} style={{ cursor: 'pointer' }} onClick={() => setSelectedExecution(e.id)}>
                    <td>{new Date(e.startedAt).toLocaleString()}</td>
                    <td><StatusBadge status={e.status} /></td>
                    <td>{e.idempotencyKey || '—'}</td>
                    <td>{selectedExecution === e.id ? '(viewing on DAG above)' : ''}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  );
}
