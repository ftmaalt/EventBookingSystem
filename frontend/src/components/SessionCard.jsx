import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { waitlistApi } from "../api";
import { useAuth } from "../auth";
import { formatDate, getError } from "../utils/format";
import { isBookable, isWaitlistable } from "../utils/sessions";
import { Feedback, StatusPill } from "./ui";

function WaitlistControls({ sessionId, onSessionRefresh }) {
  const [entry, setEntry] = useState(null);
  const [checking, setChecking] = useState(true);
  const [checkFailed, setCheckFailed] = useState(false);
  const [revision, setRevision] = useState(0);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const submitting = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    setChecking(true);
    setCheckFailed(false);
    waitlistApi.getMine(sessionId, { signal: controller.signal })
      .then(({ data }) => { if (!cancelled) setEntry(data); })
      .catch((err) => {
        if (cancelled) return;
        // The API uses 404 to represent no active WAITING entry.
        if (err.response?.status === 404) setEntry(null);
        else { setError(getError(err)); setCheckFailed(true); }
      })
      .finally(() => { if (!cancelled) setChecking(false); });
    return () => { cancelled = true; controller.abort(); };
  }, [sessionId, revision]);

  const joinWaitlist = async () => {
    if (submitting.current || checking || checkFailed) return;
    submitting.current = true;
    setBusy(true);
    setError("");
    try {
      const { data } = await waitlistApi.join(sessionId);
      setEntry(data);
    } catch (err) {
      setError(getError(err));
      if (err.response?.status === 409) {
        // Reconcile another tab's join, or a seat that opened during this request.
        setRevision((value) => value + 1);
        onSessionRefresh?.();
      }
    } finally {
      submitting.current = false;
      setBusy(false);
    }
  };

  return (
    <div className="waitlist-controls" aria-busy={checking || busy}>
      <div className="button-row">
        {checkFailed ? <button className="secondary-button" onClick={() => {
          setError(""); setRevision((value) => value + 1);
        }}>Retry waitlist status</button> : entry ?
          <Link className="secondary-button" to="/bookings?tab=waitlisted">View waitlist</Link> :
          <button className="primary-button" disabled={checking || busy} onClick={joinWaitlist}>
            {checking ? "Checking waitlist…" : busy ? "Joining…" : "Join waitlist"}
          </button>}
      </div>
      <Feedback error={error} />
    </div>
  );
}

export default function SessionCard({ session, activityActive, returnTo, onSessionRefresh }) {
  const { token, role, user } = useAuth();
  const bookable = isBookable(session);
  const waitlistable = activityActive && isWaitlistable(session);
  const activeCustomer = role === "USER" && user?.status === "ACTIVE";

  let action = <button className="secondary-button" disabled>Unavailable</button>;
  if (bookable) {
    action = <Link className="primary-button" to={token ? `/book/${session.id}` : "/login"}
      state={token ? undefined : { from: `/book/${session.id}` }}>{token ? "Book Now" : "Login to book"}</Link>;
  } else if (waitlistable && !token) {
    action = <Link className="primary-button" to="/login" state={{ from: returnTo }}>Login to join waitlist</Link>;
  } else if (waitlistable && activeCustomer) {
    action = <WaitlistControls key={user.id} sessionId={session.id} onSessionRefresh={onSessionRefresh} />;
  }

  return (
    <div className="session-card">
      <div><strong>{formatDate(session.startTime)}</strong><span>Until {formatDate(session.endTime)}</span>
        <span>{session.spotsLeft} of {session.capacity} spots remaining</span>
        {waitlistable && token && !activeCustomer && <span>Waitlists are available to active customer accounts.</span>}
      </div>
      <div className={`session-right${waitlistable ? " session-waitlist" : ""}`}>
        <StatusPill status={session.status} />
        {action}
      </div>
    </div>
  );
}
