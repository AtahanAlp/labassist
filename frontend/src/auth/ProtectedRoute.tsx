import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from './AuthContext';

/** Gate for authenticated routes; redirects to /login when there is no session. */
export function ProtectedRoute() {
  const { user } = useAuth();
  return user ? <Outlet /> : <Navigate to="/login" replace />;
}
