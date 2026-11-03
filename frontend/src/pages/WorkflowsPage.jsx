import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import StateMessage from '../components/StateMessage';
import CreateWorkflowModal from '../components/CreateWorkflowModal';

export default function WorkflowsPage() {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [showModal, setShowModal] = useState(false);
  const [createError, setCreateError] = useState('');
  const navigate = useNavigate();

  async function load() {
    try {
      setWorkflows(await api.listWorkflows());
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  return (
    <div>
      <div className="topbar">
        <h1>Workflows</h1>
        <button className="btn" onClick={() => setShowModal(true)}>+ New workflow</button>
      </div>

      {createError && <div className="auth-error" style={{ marginBottom: 12 }}>{createError}</div>}
      <StateMessage loading={loading} error={error} empty={!loading && !error && workflows.length === 0} emptyText="No workflows yet — create a DAG to get started." />

      {!loading && !error && workflows.length > 0 && (
        <div className="workflow-cards">
          {workflows.map((w) => (
            <div key={w.id} className="workflow-card" style={{ cursor: 'pointer' }} onClick={() => navigate(`/workflows/${w.id}`)}>
              <h3>{w.name}</h3>
              <div className="meta">{w.steps?.length || 0} step(s)</div>
              <button className="btn secondary" onClick={(e) => { e.stopPropagation(); navigate(`/workflows/${w.id}`); }}>View DAG</button>
            </div>
          ))}
        </div>
      )}

      {showModal && (
        <CreateWorkflowModal
          onClose={() => setShowModal(false)}
          onCreate={async (payload) => {
            try {
              await api.createWorkflow(payload);
              await load();
            } catch (err) {
              setCreateError(err.message);
              throw err;
            }
          }}
        />
      )}
    </div>
  );
}
