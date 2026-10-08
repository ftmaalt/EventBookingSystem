import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { bookingsApi } from "../api";
import { useAuth } from "../auth";
import Layout from "../components/Layout";

function Stat({ value, label }) {
  return (
    <div className="stat-card">
      <strong>{value}</strong>
      <span>{label}</span>
    </div>
  );
}

function QuickCard({ to, title, text }) {
  return (
    <Link to={to} className="quick-card">
      <div>
        <h3>{title}</h3>
        <p>{text}</p>
      </div>
      <span>→</span>
    </Link>
  );
}

export default function Dashboard() {
  const { user, role, notifications } = useAuth();
  const [upcoming, setUpcoming] = useState("—");

  useEffect(() => {
    bookingsApi
      .getAll({ status: "CONFIRMED", page: 0, size: 1 })
      .then((response) => setUpcoming(response.data.totalElements ?? 0))
      .catch(() => setUpcoming("—"));
  }, []);

  const unread = notifications.filter((item) => !item.read).length;

  return (
    <Layout>
      <div className="page">
        <div className="dashboard-header">
          <div>
            <span className="eyebrow">DASHBOARD</span>
            <h1>Welcome, {user?.fullName || "there"}</h1>
            <p>Choose what you want to do next.</p>
          </div>
          <Link className="primary-button" to="/activities">
            Explore Activities
          </Link>
        </div>

        <div className="stats-grid">
          <Stat value={upcoming} label="Confirmed bookings" />
          <Stat value={unread} label="Unread notifications" />
          <Stat value={role} label="Current role" />
          <Stat value={user?.status || "—"} label="Account status" />
        </div>

        <div className="quick-grid">
          <QuickCard to="/activities" title="Explore Activities" text="Browse available activities and sessions." />
          <QuickCard to="/bookings" title="My Bookings" text="View your current and previous bookings." />
          <QuickCard to="/profile" title="Profile" text="Update your personal information and password." />
          {role === "USER" && (
            <QuickCard
              to="/apply-provider"
              title="Become a Provider"
              text="Apply to host activities and create memorable experiences."
            />
          )}
          {role === "PROVIDER" && (
            <QuickCard to="/provider" title="Provider tools" text="Manage your activities, sessions and locations." />
          )}
          {role === "ADMIN" && (
            <QuickCard to="/admin" title="Admin Panel" text="Manage users, applications, categories and audit logs." />
          )}
        </div>
      </div>
    </Layout>
  );
}
