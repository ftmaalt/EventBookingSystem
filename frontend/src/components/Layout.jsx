import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

import { useAuth } from "../auth";
import NotificationCenter from "./NotificationCenter";
import NotificationToasts from "./NotificationToasts";

function navLinksFor(token, role) {
  if (!token) {
    return [{ to: "/activities", label: "Explore" }];
  }

  const links = [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/activities", label: "Explore" },
    { to: "/bookings", label: "My Bookings" },
    { to: "/profile", label: "Profile" },
  ];

  if (role === "USER") {
    links.push({ to: "/apply-provider", label: "Become a Provider" });
  }
  if (role === "PROVIDER") {
    links.push({ to: "/provider", label: "Provider" });
  }
  if (role === "ADMIN") {
    links.push({ to: "/admin", label: "Admin" });
  }
  return links;
}

/** Page chrome: navigation bar, footer and real-time toasts. */
export default function Layout({ children }) {
  const { token, role, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="app-shell">
      <header className="navbar">
        <Link className="logo" to={token ? "/dashboard" : "/"}>
          <span className="logo-mark">BN</span>
          <span>
            Book<span>N</span>Go
          </span>
        </Link>

        <button className="mobile-menu" aria-label="Toggle menu" onClick={() => setMobileOpen(!mobileOpen)}>
          ☰
        </button>

        <nav className={mobileOpen ? "navigation open" : "navigation"}>
          {navLinksFor(token, role).map((link) => (
            <Link key={link.to} to={link.to} onClick={() => setMobileOpen(false)}>
              {link.label}
            </Link>
          ))}

          {!token && (
            <>
              <Link to="/login" onClick={() => setMobileOpen(false)}>
                Login
              </Link>
              <Link className="nav-button" to="/register" onClick={() => setMobileOpen(false)}>
                Register
              </Link>
            </>
          )}

          {token && <NotificationCenter />}

          {token && (
            <button className="logout-button" onClick={handleLogout}>
              Logout
            </button>
          )}
        </nav>
      </header>

      <main>{children}</main>

      <NotificationToasts />

      <footer>Book N Go · Activity Booking Platform</footer>
    </div>
  );
}
