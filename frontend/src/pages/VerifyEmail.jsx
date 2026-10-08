import { useEffect, useRef, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";

import { authApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Loading } from "../components/ui";
import { getError } from "../utils/format";

/** Landing page for the link in the verification email (/verify-email?token=...). */
export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get("token");

  const [state, setState] = useState(token ? "loading" : "error");
  const [message, setMessage] = useState(token ? "" : "This verification link is missing its token.");
  // React StrictMode runs effects twice in development; a token can only be used once.
  const requested = useRef(false);

  useEffect(() => {
    if (!token || requested.current) {
      return;
    }
    requested.current = true;

    authApi
      .verify(token)
      .then((response) => {
        setMessage(response.data.message);
        setState("success");
      })
      .catch((error) => {
        setMessage(getError(error));
        setState("error");
      });
  }, [token]);

  return (
    <Layout>
      {state === "loading" ? (
        <Loading />
      ) : (
        <div className="auth-page">
          <div className="auth-card">
            <span className="eyebrow">EMAIL VERIFICATION</span>
            <h1>{state === "success" ? "Email verified" : "Verification failed"}</h1>
            <Feedback error={state === "error" ? message : ""} message={state === "success" ? message : ""} />
            <Link className="primary-button full" to="/login">
              Go to login
            </Link>
          </div>
        </div>
      )}
    </Layout>
  );
}
