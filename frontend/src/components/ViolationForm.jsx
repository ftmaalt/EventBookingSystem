import { useState } from "react";

import { violationsApi } from "../api";
import { getError } from "../utils/format";
import { Feedback, Input, Select, TextArea } from "./ui";

const TYPES = [
  ["NO_SHOW", "No show"],
  ["LATE_CANCELLED", "Late cancellation"],
  ["PROVIDER_CANCELLATION", "Provider cancellation"],
];

const EMPTY_FORM = { userId: "", type: "", excused: "false", note: "", bookingId: "", sessionId: "" };

/** Form for reporting a customer/provider violation. Shared by the provider and admin pages. */
export default function ViolationForm() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.userId || !form.type) {
      setError("User ID and violation type are required.");
      return;
    }

    try {
      const response = await violationsApi.report({
        userId: Number(form.userId),
        type: form.type,
        excused: form.excused === "true",
        note: form.note || null,
        bookingId: form.bookingId ? Number(form.bookingId) : null,
        sessionId: form.sessionId ? Number(form.sessionId) : null,
      });
      setMessage(response.data.message);
      setForm(EMPTY_FORM);
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <form className="form-card" onSubmit={submit}>
      <h2>Report a violation</h2>
      <p className="muted">Providers can only report violations on their own sessions, so a session ID is required.</p>

      <Feedback error={error} message={message} />

      <Input label="User ID" type="number" min="1" value={form.userId} onChange={(value) => update("userId", value)} />
      <Select label="Violation type" value={form.type} onChange={(value) => update("type", value)} options={TYPES} />
      <Select
        label="Excused?"
        value={form.excused}
        onChange={(value) => update("excused", value)}
        options={[
          ["false", "No (counts as a strike)"],
          ["true", "Yes (excused)"],
        ]}
      />
      <Input label="Session ID" type="number" min="1" value={form.sessionId} onChange={(value) => update("sessionId", value)} />
      <Input label="Booking ID (optional)" type="number" min="1" value={form.bookingId} onChange={(value) => update("bookingId", value)} />
      <TextArea label="Note (optional)" value={form.note} onChange={(value) => update("note", value)} />

      <button className="primary-button">Submit report</button>
    </form>
  );
}
