import { Navigate, useLocation } from "react-router-dom";

import { useAuth } from "../auth";
import { Loading } from "./ui";

/**
 * Route guard. Sends guests to the login page (remembering where they were
 * heading) and sends users without one of the allowed roles to the dashboard.
 */
export default function Protected({ children, roles }) {
  const { token, role, loading } = useAuth();
  const location = useLocation();

  if (loading) {
    return <Loading />;
  }
  if (!token) {
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (roles && !roles.includes(role)) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
}
