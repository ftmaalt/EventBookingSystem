import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

import { authApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Input } from "../components/ui";
import { getError } from "../utils/format";

/** Landing page for the link in the reset email (/reset-password?token=...). */
export default function ResetPassword() {
  const [params] = useSearchParams();
  const token = params.get("token");
  const navigate = useNavigate();

  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (newPassword.length < 8) {
      setError("Password must contain at least 8 characters.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.resetPassword({ token, newPassword });
      navigate("/login", { replace: true, state: { message: response.data.message } });
    } catch (err) {
      setError(getError(err));
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <Layout>
        <div className="auth-page">
          <div className="auth-card">
            <h1>Invalid link</h1>
            <Feedback error="This reset link is missing its token. Please request a new one." />
            <Link className="primary-button full" to="/forgot-password">
              Request a new link
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
          <span className="eyebrow">PASSWORD RECOVERY</span>
          <h1>Choose a new password</h1>

          <Input
            label="New password"
            type="password"
            value={newPassword}
            onChange={setNewPassword}
            autoComplete="new-password"
          />
          <Input
            label="Confirm new password"
            type="password"
            value={confirmPassword}
            onChange={setConfirmPassword}
            autoComplete="new-password"
          />
          <Feedback error={error} />

          <button className="primary-button full" disabled={loading}>
            {loading ? "Saving..." : "Reset password"}
          </button>

          <p className="auth-switch">
            Link expired? <Link to="/forgot-password">Request a new one</Link>
          </p>
        </form>
      </div>
    </Layout>
  );
}
