import { useState } from "react";
import { Link } from "react-router-dom";

import { authApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Input } from "../components/ui";
import { getError } from "../utils/format";

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/;

function PasswordRules({ password }) {
  const valid = password.length >= 8;
  return (
    <div className={valid ? "password-rules valid" : "password-rules"}>
      {valid ? "✓" : "○"} At least 8 characters
    </div>
  );
}

export default function Register() {
  const [form, setForm] = useState({ fullname: "", email: "", password: "", phone: "" });
  const [loading, setLoading] = useState(false);
  const [registered, setRegistered] = useState(false);
  const [error, setError] = useState("");

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (!form.fullname.trim() || !form.email.trim() || !form.password || !form.phone) {
      setError("Please complete all fields.");
      return;
    }
    if (!EMAIL_PATTERN.test(form.email)) {
      setError("Please enter a valid email address.");
      return;
    }
    if (form.password.length < 8) {
      setError("Password must contain at least 8 characters.");
      return;
    }
    if (!/^\d{8}$/.test(form.phone)) {
      setError("Phone number must be exactly 8 digits.");
      return;
    }

    setLoading(true);
    try {
      await authApi.register({ ...form, fullname: form.fullname.trim(), email: form.email.trim() });
      setRegistered(true);
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  };

  if (registered) {
    return (
      <Layout>
        <div className="auth-page">
          <div className="auth-card">
            <span className="eyebrow">ALMOST THERE</span>
            <h1>Check your email</h1>
            <p className="muted">
              We sent a verification link to <strong>{form.email}</strong>. Open it to activate your account, then
              log in.
            </p>
            <Link className="primary-button full" to="/login">
              Go to login
            </Link>
          </div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="auth-page">
        <form className="auth-card" onSubmit={submit}>
          <span className="eyebrow">JOIN BOOK N GO</span>
          <h1>Create account</h1>
          <p className="muted">Create an account to start booking activities.</p>

          <Input label="Full name" value={form.fullname} onChange={(value) => update("fullname", value)} />
          <Input label="Email" type="email" value={form.email} onChange={(value) => update("email", value)} />
          <Input
            label="Phone"
            type="tel"
            inputMode="numeric"
            maxLength={8}
            placeholder="e.g. 33333333"
            hint="Enter an 8-digit phone number"
            value={form.phone}
            onChange={(value) => update("phone", value.replace(/\D/g, ""))}
          />
          <Input
            label="Password"
            type="password"
            value={form.password}
            onChange={(value) => update("password", value)}
            autoComplete="new-password"
          />
          <PasswordRules password={form.password} />

          <Feedback error={error} />

          <button className="primary-button full" disabled={loading}>
            {loading ? "Creating..." : "Create Account"}
          </button>

          <p className="auth-switch">
            Already registered? <Link to="/login">Login</Link>
          </p>
        </form>
      </div>
    </Layout>
  );
}
