import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";

import { activitiesApi, bookingsApi, sessionsApi } from "../api";
import Layout from "../components/Layout";
import { Feedback, Loading } from "../components/ui";
import { formatDate, formatMoney, getError } from "../utils/format";
import { isBookable } from "../utils/sessions";

export default function Booking() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [session, setSession] = useState(null);
  const [activity, setActivity] = useState(null);
  const [participants, setParticipants] = useState(1);
  const [bookingType, setBookingType] = useState("INDIVIDUAL");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    sessionsApi
      .getById(id)
      .then(async (sessionResponse) => {
        const activityResponse = await activitiesApi.getById(sessionResponse.data.activityId);
        if (!cancelled) {
          setSession(sessionResponse.data);
          setActivity(activityResponse.data);
        }
      })
      .catch((err) => !cancelled && setError(getError(err)))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [id]);

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (!Number.isInteger(participants) || participants < 1) {
      setError("You must select at least one participant.");
      return;
    }
    if (participants > session.spotsLeft) {
      setError(`Only ${session.spotsLeft} spots are available.`);
      return;
    }

    setSubmitting(true);
    try {
      await bookingsApi.create({ sessionId: Number(id), participants, bookingType });
      navigate("/bookings", { state: { message: "Your booking is confirmed. A confirmation email is on its way." } });
    } catch (err) {
      setError(getError(err));
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

  if (!session || !activity) {
    return (
      <Layout>
        <div className="page narrow-page">
          <Link className="back-link" to="/activities">
            ← Back to activities
          </Link>
          <Feedback error={error || "Session not found."} />
        </div>
      </Layout>
    );
  }

  const bookable = isBookable(session);
  const total = Number(activity.pricePerPerson) * (Number.isInteger(participants) ? participants : 0);

  return (
    <Layout>
      <div className="page narrow-page">
        <Link className="back-link" to={`/activities/${activity.activity_id}`}>
          ← Back
        </Link>

        <div className="booking-card">
          <span className="eyebrow">RESERVATION</span>
          <h1>Book your session</h1>

          <div className="booking-summary">
            <strong>{activity.title}</strong>
            <span>{formatDate(session.startTime)}</span>
            <span>{formatMoney(activity.pricePerPerson)} per person</span>
          </div>

          {!bookable && <div className="error-box">This session is no longer available for booking.</div>}

          <form onSubmit={submit}>
            <label className="field">
              <span>Participants</span>
              <input
                type="number"
                min="1"
                max={session.spotsLeft}
                value={Number.isNaN(participants) ? "" : participants}
                onChange={(event) => setParticipants(event.target.valueAsNumber)}
                disabled={!bookable}
              />
              <small>{session.spotsLeft} spots available</small>
            </label>

            <label className="field">
              <span>Booking Type</span>
              <select value={bookingType} onChange={(event) => setBookingType(event.target.value)} disabled={!bookable}>
                <option value="INDIVIDUAL">Individual</option>
                <option value="GROUP">Group</option>
              </select>
            </label>

            <p>
              Total: <strong>{formatMoney(total)}</strong>
            </p>

            <Feedback error={error} />

            <button className="primary-button full" disabled={submitting || !bookable}>
              {submitting ? "Booking..." : "Confirm Booking"}
            </button>
          </form>
        </div>
      </div>
    </Layout>
  );
}
