import { useEffect, useState } from 'react';
import { api } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import PriorityBadge from '../components/PriorityBadge';
import StateMessage from '../components/StateMessage';

export default function DashboardPage() {
  const [jobs, setJobs] = useState([]);
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  async function load() {
    try {
      const [j, w] = await Promise.all([api.listJobs(), api.listWorkflows()]);
      setJobs(j);
      setWorkflows(w);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    load();
    const interval = setInterval(load, 5000);
    return () => clearInterval(interval);
  }, []);

  const active = jobs.filter((j) => j.status === 'ACTIVE').length;
  const paused = jobs.filter((j) => j.status === 'PAUSED').length;

  return (
    <div>
      <div className="topbar"><h1>Dashboard</h1></div>

      <div className="cards-row">
        <div className="card"><div className="label">Total jobs</div><div className="value">{jobs.length}</div></div>
        <div className="card"><div className="label">Active</div><div className="value">{active}</div></div>
        <div className="card"><div className="label">Paused</div><div className="value">{paused}</div></div>
        <div className="card"><div className="label">Workflows</div><div className="value">{workflows.length}</div></div>
      </div>

      <h2 style={{ fontSize: 14, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.04em' }}>
        Live jobs
      </h2>
      <StateMessage loading={loading} error={error} empty={!loading && !error && jobs.length === 0} emptyText="No jobs yet — create one from the Jobs page." />
      {!loading && !error && jobs.length > 0 && (
        <table>
          <thead>
            <tr><th>Name</th><th>Type</th><th>Schedule</th><th>Priority</th><th>Status</th><th>Next fire</th></tr>
          </thead>
          <tbody>
            {jobs.slice(0, 10).map((j) => (
              <tr key={j.id}>
                <td>{j.name}</td>
                <td>{j.taskType}</td>
                <td>{j.scheduleType}</td>
                <td><PriorityBadge priority={j.priority} /></td>
                <td><StatusBadge status={j.status} /></td>
                <td>{j.nextFireAt ? new Date(j.nextFireAt).toLocaleString() : '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
