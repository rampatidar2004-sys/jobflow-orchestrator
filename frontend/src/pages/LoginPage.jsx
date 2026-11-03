import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function LoginPage() {
  const { login, register } = useAuth();
  const navigate = useNavigate();
  const [mode, setMode] = useState('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(e) {
    e.preventDefault();
    setError('');
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await login(username, password);
      } else {
        await register(username, password);
      }
      navigate('/');
    } catch (err) {
      setError(mode === 'login' ? 'Invalid username or password.' : (err.message || 'Registration failed.'));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-screen">
      <div className="auth-card">
        <h1>JobFlow Orchestrator</h1>
        <p className="subtitle">Platform engineering console</p>
        <form onSubmit={submit}>
          {error && <div className="auth-error">{error}</div>}
          <div className="form-field">
            <label>Username</label>
            <input required value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
          </div>
          <div className="form-field">
            <label>Password</label>
            <input required type="password" minLength={mode === 'register' ? 8 : undefined}
                   value={password} onChange={(e) => setPassword(e.target.value)} />
          </div>
          <button className="btn" type="submit" style={{ width: '100%' }} disabled={submitting}>
            {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>
        </form>
        <div className="auth-toggle">
          {mode === 'login' ? (
            <>No account? <a href="#" onClick={(e) => { e.preventDefault(); setMode('register'); }}>Register</a></>
          ) : (
            <>Have an account? <a href="#" onClick={(e) => { e.preventDefault(); setMode('login'); }}>Sign in</a></>
          )}
        </div>
      </div>
    </div>
  );
}
