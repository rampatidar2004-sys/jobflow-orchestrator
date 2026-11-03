import { NavLink } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const LINKS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/jobs', label: 'Jobs' },
  { to: '/workflows', label: 'Workflows' },
  { to: '/architecture', label: 'Architecture' }
];

export default function Sidebar() {
  const { logout } = useAuth();
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">JobFlow<span>::</span>Orchestrator</div>
      {LINKS.map((l) => (
        <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => `nav-link${isActive ? ' active' : ''}`}>
          {l.label}
        </NavLink>
      ))}
      <div style={{ flex: 1 }} />
      <button className="nav-link" style={{ textAlign: 'left', background: 'none', border: 'none', cursor: 'pointer' }} onClick={logout}>
        Sign out
      </button>
    </aside>
  );
}
