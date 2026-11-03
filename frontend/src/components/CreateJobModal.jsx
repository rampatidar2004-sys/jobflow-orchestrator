import { useState } from 'react';

const DEFAULTS = {
  name: '',
  taskType: 'demo',
  scheduleType: 'ONCE',
  cronExpression: '',
  intervalSeconds: 60,
  priority: 5,
  maxAttempts: 5,
  payload: '{\n  "durationMs": 500,\n  "failUntilAttempt": 1\n}'
};

export default function CreateJobModal({ onClose, onCreate }) {
  const [form, setForm] = useState(DEFAULTS);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  function set(field, value) {
    setForm((f) => ({ ...f, [field]: value }));
  }

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      JSON.parse(form.payload || '{}');
      await onCreate({
        name: form.name,
        taskType: form.taskType,
        scheduleType: form.scheduleType,
        cronExpression: form.scheduleType === 'CRON' ? form.cronExpression : undefined,
        intervalSeconds: form.scheduleType === 'INTERVAL' ? Number(form.intervalSeconds) : undefined,
        priority: Number(form.priority),
        maxAttempts: Number(form.maxAttempts),
        payload: form.payload
      });
      onClose();
    } catch (err) {
      setError(err.message || 'failed to create job');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>New job</h2>
        <form onSubmit={submit}>
          {error && <div className="auth-error">{error}</div>}
          <div className="form-field">
            <label>Name</label>
            <input required value={form.name} onChange={(e) => set('name', e.target.value)} placeholder="nightly-report" />
          </div>
          <div className="form-row">
            <div className="form-field">
              <label>Task type</label>
              <input required value={form.taskType} onChange={(e) => set('taskType', e.target.value)} />
            </div>
            <div className="form-field">
              <label>Schedule</label>
              <select value={form.scheduleType} onChange={(e) => set('scheduleType', e.target.value)}>
                <option value="ONCE">Once</option>
                <option value="INTERVAL">Interval</option>
                <option value="CRON">Cron</option>
              </select>
            </div>
          </div>
          {form.scheduleType === 'CRON' && (
            <div className="form-field">
              <label>Cron expression</label>
              <input value={form.cronExpression} onChange={(e) => set('cronExpression', e.target.value)} placeholder="*/5 * * * *" />
            </div>
          )}
          {form.scheduleType === 'INTERVAL' && (
            <div className="form-field">
              <label>Interval (seconds)</label>
              <input type="number" min="1" value={form.intervalSeconds} onChange={(e) => set('intervalSeconds', e.target.value)} />
            </div>
          )}
          <div className="form-row">
            <div className="form-field">
              <label>Priority (1-10)</label>
              <input type="number" min="1" max="10" value={form.priority} onChange={(e) => set('priority', e.target.value)} />
            </div>
            <div className="form-field">
              <label>Max attempts</label>
              <input type="number" min="1" value={form.maxAttempts} onChange={(e) => set('maxAttempts', e.target.value)} />
            </div>
          </div>
          <div className="form-field">
            <label>Payload (JSON)</label>
            <textarea value={form.payload} onChange={(e) => set('payload', e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 10 }}>
            <button type="button" className="btn secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn" disabled={submitting}>{submitting ? 'Creating…' : 'Create job'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
