import { useEffect, useRef, useState } from "react";
import { Link } from "react-router-dom";

import { waitlistApi } from "../api";
import { formatDate, getError } from "../utils/format";
import { EmptyState, Feedback, Loading, Pagination, StatusPill } from "./ui";

export default function WaitlistedBookings() {
  const [entries, setEntries] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [leavingId, setLeavingId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const submitting = useRef(false);

  useEffect(() => {
    let cancelled = false;
    const controller = new AbortController();
    setLoading(true);
    waitlistApi.getAll({ page, size: 10 }, { signal: controller.signal })
      .then(({ data }) => {
        if (cancelled) return;
        const lastPage = Math.max(0, (data.totalPages || 0) - 1);
        if (page > lastPage) { setPage(lastPage); return; }
        setEntries(data.content || []);
        setTotalPages(data.totalPages || 0);
        setError("");
      })
      .catch((err) => { if (!cancelled) setError(getError(err)); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; controller.abort(); };
  }, [page, revision]);

  const leave = async (entry) => {
    if (submitting.current) return;
    submitting.current = true;
    setLeavingId(entry.entryId);
    setError("");
    setMessage("");
    try {
      await waitlistApi.leave(entry.sessionId);
      setMessage(`You have left the waitlist for ${entry.activityTitle}.`);
      setRevision((value) => value + 1);
    } catch (err) {
      setError(getError(err));
      if (err.response?.status === 404) {
        // Allocation or leaving from another tab can remove an active entry.
        setMessage("This waitlist entry is no longer active. Check your confirmed bookings if a spot was assigned.");
        setRevision((value) => value + 1);
      }
    } finally {
      submitting.current = false;
      setLeavingId(null);
    }
  };

  return (
    <section aria-label="Waitlisted sessions" aria-busy={loading || leavingId !== null}>
      <div className="waitlisted-toolbar"><p className="muted">Your waiting sessions and current queue positions.</p>
        <button className="secondary-button" disabled={loading || leavingId !== null}
          onClick={() => setRevision((value) => value + 1)}>Refresh positions</button></div>
      <Feedback error={error} message={message} />
      {loading ? <Loading /> : entries.length === 0 ?
        <EmptyState title="No waitlisted sessions" text="Sessions you join a waitlist for will appear here." /> :
        <div className="booking-list">{entries.map((entry) => <article className="booking-card-row" key={entry.entryId}>
          <div className="booking-icon" aria-hidden="true">⌛</div>
          <div className="booking-info">
            <h3><Link to={`/activities/${entry.activityId}`}>{entry.activityTitle}</Link></h3>
            <p>{formatDate(entry.startTime)}</p><span>Until {formatDate(entry.endTime)}</span>
            <span>Joined {formatDate(entry.joinedAt)}</span>
          </div>
          <div className="booking-price"><strong>Position {entry.position}</strong><StatusPill status="WAITLISTED" />
            <button className="secondary-button" disabled={leavingId !== null} onClick={() => leave(entry)}>
              {leavingId === entry.entryId ? "Leaving…" : "Leave waitlist"}</button>
          </div>
        </article>)}</div>}
      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </section>
  );
}
