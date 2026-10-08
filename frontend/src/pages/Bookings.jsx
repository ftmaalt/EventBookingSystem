import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation } from "react-router-dom";

import { activitiesApi, bookingsApi, sessionsApi } from "../api";
import Layout from "../components/Layout";
import { EmptyState, Feedback, Loading, Pagination, SectionHeader, StatusPill } from "../components/ui";
import { formatDate, formatMoney, getError, labelize } from "../utils/format";

const PAGE_SIZE = 10;
// New bookings are confirmed immediately, so there is no pending-payment status to filter on.
const STATUS_TABS = ["", "CONFIRMED", "COMPLETED", "CANCELLED"];

/** Loads the session and activity behind each booking so the list can show real names and times. */
async function loadDetails(bookings, known) {
  const details = { ...known };
  const missing = [...new Set(bookings.map((booking) => booking.sessionId))].filter((id) => !details[id]);

  await Promise.all(
    missing.map(async (sessionId) => {
      try {
        const session = (await sessionsApi.getById(sessionId)).data;
        const activity = (await activitiesApi.getById(session.activityId)).data;
        details[sessionId] = { session, activity };
      } catch {
        details[sessionId] = null;
      }
    })
  );
  return details;
}

function BookingCard({ booking, detail, onCancel }) {
  return (
    <div className="booking-card-row">
      <div className="booking-icon">▣</div>

      <div className="booking-info">
        <h3>
          {detail?.activity ? (
            <Link to={`/activities/${detail.activity.activity_id}`}>{detail.activity.title}</Link>
          ) : (
            `Booking #${booking.id}`
          )}
        </h3>
        <p>
          Booking #{booking.id}
          {detail?.session && ` · ${formatDate(detail.session.startTime)}`}
        </p>
        <span>
          {booking.participants} participant{booking.participants !== 1 ? "s" : ""}
        </span>
        <span>{labelize(booking.bookingType)}</span>
        <span>Booked {formatDate(booking.createdAt)}</span>
      </div>

      <div className="booking-price">
        <strong>{formatMoney(booking.totalPrice)}</strong>
        <StatusPill status={booking.status} />
        <small>Payment: {labelize(booking.paymentStatus)}</small>
        {booking.status === "CONFIRMED" && (
          <button className="danger-button" onClick={() => onCancel(booking.id)}>
            Cancel
          </button>
        )}
      </div>
    </div>
  );
}

export default function Bookings() {
  const location = useLocation();

  const [bookings, setBookings] = useState([]);
  const [details, setDetails] = useState({});
  // Cache of session/activity details, kept in a ref so reloading the list does not depend on it.
  const detailCache = useRef({});
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState(location.state?.message || "");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const response = await bookingsApi.getAll({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        sort: "createdAt,desc",
      });
      const items = response.data.content || [];
      setBookings(items);
      setTotalPages(response.data.totalPages || 0);
      detailCache.current = await loadDetails(items, detailCache.current);
      setDetails(detailCache.current);
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  }, [page, status]);

  useEffect(() => {
    load();
  }, [load]);

  const cancel = async (bookingId) => {
    if (!window.confirm("Are you sure you want to cancel this booking?")) {
      return;
    }
    setError("");
    setMessage("");
    try {
      await bookingsApi.cancel(bookingId);
      setMessage(`Booking #${bookingId} was cancelled.`);
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  const changeStatus = (value) => {
    setStatus(value);
    setPage(0);
  };

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="My Bookings" text="Track and manage your reservations." />

        <div className="tabs">
          {STATUS_TABS.map((value) => (
            <button key={value} className={status === value ? "active" : ""} onClick={() => changeStatus(value)}>
              {value ? labelize(value) : "ALL"}
            </button>
          ))}
        </div>

        <Feedback error={error} message={message} />

        {loading ? (
          <Loading />
        ) : bookings.length === 0 ? (
          <EmptyState title="No bookings" text="Your bookings will appear here." />
        ) : (
          <div className="booking-list">
            {bookings.map((booking) => (
              <BookingCard key={booking.id} booking={booking} detail={details[booking.sessionId]} onCancel={cancel} />
            ))}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </Layout>
  );
}
