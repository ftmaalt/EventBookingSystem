import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";

import Layout from "../components/Layout";
import { Feedback, Input } from "../components/ui";
import { useAuth } from "../auth";
import { getError } from "../utils/format";

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Message passed by other pages, e.g. after verifying an email or resetting a password.
  const notice = location.state?.message || "";

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (!email.trim() || !password) {
      setError("Email and password are required.");
      return;
    }

    setLoading(true);
    try {
      const result = await login({ email: email.trim(), password });
      const fallback = result.role === "ADMIN" ? "/admin" : "/dashboard";
      navigate(location.state?.from || fallback, { replace: true });
    } catch (err) {
      setError(getError(err));
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="auth-page">
        <form className="auth-card" onSubmit={submit}>
          <span className="eyebrow">BOOK N GO</span>
          <h1>Welcome back</h1>
          <p className="muted">Login to continue.</p>

          <Feedback message={notice} error={error} />

          <Input label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
          <Input
            label="Password"
            type="password"
            value={password}
            onChange={setPassword}
            autoComplete="current-password"
          />

          <p className="auth-switch">
            <Link to="/forgot-password">Forgot your password?</Link>
          </p>

          <button className="primary-button full" disabled={loading}>
            {loading ? "Logging in..." : "Login"}
          </button>

          <p className="auth-switch">
            Don't have an account? <Link to="/register">Register</Link>
          </p>
        </form>
      </div>
    </Layout>
  );
}
