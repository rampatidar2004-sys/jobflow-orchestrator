import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import RequireAuth from './components/RequireAuth';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import JobsPage from './pages/JobsPage';
import WorkflowsPage from './pages/WorkflowsPage';
import WorkflowDetailPage from './pages/WorkflowDetailPage';
import ArchitecturePage from './pages/ArchitecturePage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<RequireAuth><DashboardPage /></RequireAuth>} />
          <Route path="/jobs" element={<RequireAuth><JobsPage /></RequireAuth>} />
          <Route path="/workflows" element={<RequireAuth><WorkflowsPage /></RequireAuth>} />
          <Route path="/workflows/:id" element={<RequireAuth><WorkflowDetailPage /></RequireAuth>} />
          <Route path="/architecture" element={<RequireAuth><ArchitecturePage /></RequireAuth>} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
