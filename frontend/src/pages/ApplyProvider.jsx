import { useEffect, useState } from "react";

import { applicationsApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Input, Loading, StatusPill, TextArea } from "../components/ui";
import { formatDate, getError } from "../utils/format";

const EMPTY_FORM = {
  businessName: "",
  contactName: "",
  phone: "",
  city: "",
  description: "",
  proposedActivities: "",
};

export default function ApplyProvider() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const loadApplications = () =>
    applicationsApi
      .getMine()
      .then((response) => setApplications(response.data || []))
      .catch(() => setApplications([]));

  useEffect(() => {
    loadApplications().finally(() => setLoading(false));
  }, []);

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const latest = [...applications].sort((a, b) => String(b.createdAt).localeCompare(String(a.createdAt)))[0];
  const hasPending = latest?.status === "PENDING";

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.businessName.trim() || !form.contactName.trim() || !form.phone || !form.city.trim() || !form.proposedActivities.trim()) {
      setError("Please complete all required fields.");
      return;
    }
    if (!/^\d{8}$/.test(form.phone)) {
      setError("Phone number must be exactly 8 digits.");
      return;
    }

    setSubmitting(true);
    try {
      await applicationsApi.submit(form);
      setMessage("Provider application submitted successfully. You will be notified once it is reviewed.");
      setForm(EMPTY_FORM);
      await loadApplications();
    } catch (err) {
      setError(getError(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <Layout>
        <Loading />
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="auth-page">
        <div className="auth-card">
          <span className="eyebrow">BECOME A PROVIDER</span>
          <h1>Provider Application</h1>

          {latest && (
            <div className="application">
              <div>
                <strong>{latest.businessName}</strong>
                <span>Submitted {formatDate(latest.createdAt)}</span>
                {latest.reviewNote && <span>Reviewer note: {latest.reviewNote}</span>}
              </div>
              <StatusPill status={latest.status} />
            </div>
          )}

          <Feedback error={error} message={message} />

          {hasPending ? (
            <p className="muted provider-application-intro">
              Your application is waiting for review. We will notify you as soon as an administrator decides.
            </p>
          ) : (
            <form onSubmit={submit}>
              <p className="muted provider-application-intro">
                {latest?.status === "REJECTED"
                  ? "Your last application was not approved. You are welcome to apply again."
                  : "Tell us about the experiences you would like to host. Once approved, your account can access provider tools for creating activities and sessions."}
              </p>

              <Input label="Business name" value={form.businessName} onChange={(value) => update("businessName", value)} />
              <Input label="Contact name" value={form.contactName} onChange={(value) => update("contactName", value)} />
              <Input
                label="Phone"
                type="tel"
                inputMode="numeric"
                maxLength={8}
                hint="8 digits"
                value={form.phone}
                onChange={(value) => update("phone", value.replace(/\D/g, ""))}
              />
              <Input label="City" value={form.city} onChange={(value) => update("city", value)} />
              <Input
                label="Proposed activities"
                value={form.proposedActivities}
                onChange={(value) => update("proposedActivities", value)}
              />
              <TextArea label="Description (optional)" value={form.description} onChange={(value) => update("description", value)} />

              <button className="primary-button full" disabled={submitting}>
                {submitting ? "Submitting..." : "Submit Application"}
              </button>
            </form>
          )}
        </div>
      </div>
    </Layout>
  );
}
