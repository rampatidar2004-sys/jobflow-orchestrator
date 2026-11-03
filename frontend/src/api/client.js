const BASE = '/api';

function authHeader() {
  const token = localStorage.getItem('jobflow_token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

async function request(path, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
      ...(options.headers || {})
    }
  });
  if (res.status === 401) {
    localStorage.removeItem('jobflow_token');
    window.location.href = '/login';
    throw new Error('unauthorized');
  }
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body.message || `request failed: ${res.status}`);
  }
  if (res.status === 204) return null;
  return res.json();
}

export const api = {
  login: (username, password) =>
    request('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
  register: (username, password) =>
    request('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),

  listJobs: () => request('/jobs'),
  getJob: (id) => request(`/jobs/${id}`),
  createJob: (payload) => request('/jobs', { method: 'POST', body: JSON.stringify(payload) }),
  pauseJob: (id) => request(`/jobs/${id}/pause`, { method: 'POST' }),
  resumeJob: (id) => request(`/jobs/${id}/resume`, { method: 'POST' }),
  runJob: (id) => request(`/jobs/${id}/run`, { method: 'POST' }),
  cancelJob: (id) => request(`/jobs/${id}/cancel`, { method: 'POST' }),
  jobExecutions: (id) => request(`/jobs/${id}/executions`),

  listWorkflows: () => request('/workflows'),
  getWorkflow: (id) => request(`/workflows/${id}`),
  createWorkflow: (payload) => request('/workflows', { method: 'POST', body: JSON.stringify(payload) }),
  startWorkflow: (id, idempotencyKey) =>
    request(`/workflows/${id}/executions`, {
      method: 'POST',
      body: JSON.stringify(idempotencyKey ? { idempotencyKey } : {})
    }),
  workflowExecutions: (id) => request(`/workflows/${id}/executions`),
  workflowExecutionSteps: (executionId) => request(`/workflows/executions/${executionId}/steps`)
};
