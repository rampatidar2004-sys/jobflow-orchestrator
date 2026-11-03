import { Fragment, useEffect, useState } from 'react';
import { api } from '../api/client';
import StatusBadge from '../components/StatusBadge';
import PriorityBadge from '../components/PriorityBadge';
import StateMessage from '../components/StateMessage';
import CreateJobModal from '../components/CreateJobModal';

export default function JobsPage() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [expanded, setExpanded] = useState(null);
  const [executions, setExecutions] = useState([]);
  const [actionError, setActionError] = useState('');

  async function load() {
    try {
      setJobs(await api.listJobs());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function toggleExpand(job) {
    if (expanded === job.id) {
      setExpanded(null);
      return;
    }
    setExpanded(job.id);
    try {
      setExecutions(await api.jobExecutions(job.id));
    } catch (err) {
      setActionError(err.message);
    }
  }

  async function act(fn, id) {
    setActionError('');
    try {
      await fn(id);
      await load();
    } catch (err) {
      setActionError(err.message);
    }
  }

  return (
    <div>
      <div className="topbar">
        <h1>Jobs</h1>
        <button className="btn" onClick={() => setShowModal(true)}>+ New job</button>
      </div>

      {actionError && <div className="auth-error" style={{ marginBottom: 12 }}>{actionError}</div>}
      <StateMessage loading={loading} error={error} empty={!loading && !error && jobs.length === 0} emptyText="No jobs yet." />

      {!loading && !error && jobs.length > 0 && (
        <table>
          <thead>
            <tr><th>Name</th><th>Type</th><th>Schedule</th><th>Priority</th><th>Status</th><th>Attempts</th><th>Controls</th></tr>
          </thead>
          <tbody>
            {jobs.map((j) => (
              <Fragment key={j.id}>
                <tr style={{ cursor: 'pointer' }} onClick={() => toggleExpand(j)}>
                  <td>{j.name}</td>
                  <td>{j.taskType}</td>
                  <td>{j.scheduleType}{j.scheduleType === 'CRON' ? ` (${j.cronExpression})` : ''}</td>
                  <td><PriorityBadge priority={j.priority} /></td>
                  <td><StatusBadge status={j.status} /></td>
                  <td>{j.maxAttempts}</td>
                  <td onClick={(e) => e.stopPropagation()}>
                    <div style={{ display: 'flex', gap: 6 }}>
                      {j.status === 'ACTIVE' && <button className="btn secondary" onClick={() => act(api.pauseJob, j.id)}>Pause</button>}
                      {j.status === 'PAUSED' && <button className="btn secondary" onClick={() => act(api.resumeJob, j.id)}>Resume</button>}
                      <button className="btn secondary" onClick={() => act(api.runJob, j.id)}>Run now</button>
                      {j.status !== 'CANCELLED' && <button className="btn danger" onClick={() => act(api.cancelJob, j.id)}>Cancel</button>}
                    </div>
                  </td>
                </tr>
                {expanded === j.id && (
                  <tr>
                    <td colSpan={7} style={{ background: 'var(--bg-elevated)' }}>
                      <strong style={{ fontSize: 12, color: 'var(--text-muted)' }}>Execution history</strong>
                      {executions.length === 0 ? (
                        <div className="state-message">No executions yet.</div>
                      ) : (
                        <table style={{ marginTop: 8 }}>
                          <thead><tr><th>Attempt</th><th>Status</th><th>Worker</th><th>Started</th><th>Result / error</th></tr></thead>
                          <tbody>
                            {executions.map((ex) => (
                              <tr key={ex.id}>
                                <td>{ex.attemptNumber}</td>
                                <td><StatusBadge status={ex.status} /></td>
                                <td>{ex.workerId || '—'}</td>
                                <td>{ex.startedAt ? new Date(ex.startedAt).toLocaleString() : '—'}</td>
                                <td>{ex.errorMessage || ex.resultSummary || '—'}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      )}
                    </td>
                  </tr>
                )}
              </Fragment>
            ))}
          </tbody>
        </table>
      )}

      {showModal && (
        <CreateJobModal onClose={() => setShowModal(false)} onCreate={async (payload) => { await api.createJob(payload); await load(); }} />
      )}
    </div>
  );
}
