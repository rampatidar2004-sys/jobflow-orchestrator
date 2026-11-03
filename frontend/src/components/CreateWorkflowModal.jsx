import { useState } from 'react';

const EXAMPLE = `[
  { "stepKey": "prepare", "taskType": "demo", "dependsOn": [] },
  { "stepKey": "validate", "taskType": "demo", "dependsOn": ["prepare"] },
  { "stepKey": "enrich", "taskType": "demo", "dependsOn": ["prepare"] },
  { "stepKey": "finish", "taskType": "demo", "dependsOn": ["validate", "enrich"] }
]`;

export default function CreateWorkflowModal({ onClose, onCreate }) {
  const [name, setName] = useState('');
  const [stepsJson, setStepsJson] = useState(EXAMPLE);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      const parsed = JSON.parse(stepsJson);
      const steps = parsed.map((s) => ({ ...s, payload: s.payload || '{}' }));
      await onCreate({ name, steps });
      onClose();
    } catch (err) {
      setError(err.message || 'failed to create workflow');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <h2>New workflow</h2>
        <form onSubmit={submit}>
          {error && <div className="auth-error">{error}</div>}
          <div className="form-field">
            <label>Name</label>
            <input required value={name} onChange={(e) => setName(e.target.value)} placeholder="nightly-etl" />
          </div>
          <div className="form-field">
            <label>Steps (JSON array: stepKey, taskType, dependsOn)</label>
            <textarea style={{ minHeight: 160 }} value={stepsJson} onChange={(e) => setStepsJson(e.target.value)} />
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', marginTop: 10 }}>
            <button type="button" className="btn secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn" disabled={submitting}>{submitting ? 'Creating…' : 'Create workflow'}</button>
          </div>
        </form>
      </div>
    </div>
  );
}
