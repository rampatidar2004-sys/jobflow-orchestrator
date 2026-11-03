import { createContext, useContext, useState, useCallback } from 'react';
import { api } from '../api/client';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('jobflow_token'));

  const login = useCallback(async (username, password) => {
    const res = await api.login(username, password);
    localStorage.setItem('jobflow_token', res.token);
    setToken(res.token);
  }, []);

  const register = useCallback(async (username, password) => {
    const res = await api.register(username, password);
    localStorage.setItem('jobflow_token', res.token);
    setToken(res.token);
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('jobflow_token');
    setToken(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, isAuthenticated: !!token, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
