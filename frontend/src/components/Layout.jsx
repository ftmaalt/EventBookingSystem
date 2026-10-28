import { useEffect, useRef, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";

import { useAuth } from "../auth";
import NotificationCenter from "./NotificationCenter";
import NotificationToasts from "./NotificationToasts";
import SearchBar from "./SearchBar";

function navLinksFor(token, role) {
  if (!token) {
    return [{ to: "/activities", label: "Explore" }];
  }

  const links = [
    { to: "/dashboard", label: "Dashboard" },
    { to: "/activities", label: "Explore" },
    { to: "/bookings", label: "My Bookings" },
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
  const { token, role, user, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileOpen, setMobileOpen] = useState(false);
  const menuButton = useRef(null);
  const header = useRef(null);
  const search = (query) => {
    setMobileOpen(false);
    navigate(query ? `/activities?q=${encodeURIComponent(query)}` : "/activities");
  };

  useEffect(() => {
    if (!mobileOpen) return;
    const dismiss = (event) => {
      if (event.key === "Escape") {
        setMobileOpen(false);
        menuButton.current?.focus();
      }
    };
    const outside = (event) => {
      if (!header.current?.contains(event.target)) setMobileOpen(false);
    };
    document.addEventListener("keydown", dismiss);
    document.addEventListener("pointerdown", outside);
    return () => {
      document.removeEventListener("keydown", dismiss);
      document.removeEventListener("pointerdown", outside);
    };
  }, [mobileOpen]);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
    <div className="app-shell">
      <a href="#main-content" className="skip-link">Skip to content</a>
      <header className="navbar" ref={header}>
        <Link className="logo" to={token ? "/dashboard" : "/"} aria-label="BookNGo home">
          <span className="logo-mark" aria-hidden="true">✦</span>
          <span>
            Book<span>N</span>Go
          </span>
        </Link>

        <button className="mobile-menu" ref={menuButton} aria-label={mobileOpen ? "Close menu" : "Open menu"}
          aria-expanded={mobileOpen} aria-controls="main-navigation" onClick={() => setMobileOpen(!mobileOpen)}>
          <span aria-hidden="true">{mobileOpen ? "×" : "☰"}</span>
        </button>

        <nav id="main-navigation" aria-label="Main navigation" className={mobileOpen ? "navigation open" : "navigation"}>
          {navLinksFor(token, role).map((link) => (
            <NavLink key={link.to} to={link.to} onClick={() => setMobileOpen(false)}>
              {link.label}
            </NavLink>
          ))}

          <SearchBar compact onSearch={search} />

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

          {token && <details className="profile-menu">
            <summary><span className="profile-initial" aria-hidden="true">{user?.fullName?.charAt(0) || "B"}</span>
              <span>My account</span><span aria-hidden="true">⌄</span></summary>
            <div className="profile-dropdown">
              <small>{user?.fullName || "Your BookNGo account"}</small>
              <Link to="/profile" onClick={() => setMobileOpen(false)}>Profile</Link>
              <button className="logout-button" onClick={handleLogout}>Logout</button>
            </div>
          </details>}
        </nav>
      </header>

      <main id="main-content" tabIndex={-1}>{children}</main>

      <NotificationToasts />

      <footer className="site-footer">
        <div><Link className="footer-wordmark" to="/">BookNGo<span aria-hidden="true">✦</span></Link>
          <p>A little discovery. A lasting memory.</p></div>
        <nav aria-label="Footer navigation"><Link to="/activities">Explore experiences</Link>
          <Link to={token ? "/bookings" : "/register"}>{token ? "Your bookings" : "Join BookNGo"}</Link>
          <Link to={token ? "/profile" : "/login"}>{token ? "Your account" : "Sign in"}</Link></nav>
      </footer>
    </div>
  );
}
