import { useState } from "react";
import { Link } from "react-router-dom";

import { authApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Input } from "../components/ui";
import { getError } from "../utils/format";

export default function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setError("");
    setMessage("");

    if (!/^\S+@\S+\.\S+$/.test(email)) {
      setError("Please enter a valid email address.");
      return;
    }

    setLoading(true);
    try {
      const response = await authApi.forgotPassword({ email: email.trim() });
      setMessage(response.data.message);
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Layout>
      <div className="auth-page">
        <form className="auth-card" onSubmit={submit}>
          <span className="eyebrow">PASSWORD RECOVERY</span>
          <h1>Forgot password?</h1>
          <p className="muted">Enter your email and we will send you a link to reset your password.</p>

          <Input label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
          <Feedback error={error} message={message} />

          <button className="primary-button full" disabled={loading}>
            {loading ? "Sending..." : "Send reset link"}
          </button>

          <p className="auth-switch">
            <Link to="/login">Back to login</Link>
          </p>
        </form>
      </div>
    </Layout>
  );
}
