import { useCallback, useEffect, useRef, useState } from "react";
import { Link, useLocation, useSearchParams } from "react-router-dom";

import { activitiesApi, bookingsApi, sessionsApi } from "../api";
import Layout from "../components/Layout";
import WaitlistedBookings from "../components/WaitlistedBookings";
import { EmptyState, Feedback, Loading, Pagination, SectionHeader, StatusPill } from "../components/ui";
import { formatDate, formatMoney, getError, labelize } from "../utils/format";
import { createBookingDetailsLoader } from "../utils/bookingDetails";

const PAGE_SIZE = 10;
// New bookings are confirmed immediately, so there is no pending-payment status to filter on.
const STATUS_TABS = ["", "CONFIRMED", "COMPLETED", "CANCELLED"];

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
  const [params, setParams] = useSearchParams();
  const waitlisted = params.get("tab") === "waitlisted";

  const [bookings, setBookings] = useState([]);
  const [details, setDetails] = useState({});
  const requestVersion = useRef(0);
  const [loadDetails] = useState(() => createBookingDetailsLoader({
    loadSession: sessionsApi.getById,
    loadActivity: activitiesApi.getById,
  }));
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState(location.state?.message || "");

  const load = useCallback(async () => {
    const version = ++requestVersion.current;
    if (waitlisted) return;
    setLoading(true);
    try {
      const response = await bookingsApi.getAll({
        page,
        size: PAGE_SIZE,
        status: status || undefined,
        sort: "createdAt,desc",
      });
      if (version !== requestVersion.current) return;
      const items = response.data.content || [];
      setBookings(items);
      setTotalPages(response.data.totalPages || 0);
      const nextDetails = await loadDetails(items);
      if (version !== requestVersion.current) return;
      setDetails(nextDetails);
      setError("");
    } catch (err) {
      if (version === requestVersion.current) setError(getError(err));
    } finally {
      if (version === requestVersion.current) setLoading(false);
    }
  }, [page, status, loadDetails, waitlisted]);

  useEffect(() => {
    load();
    return () => { ++requestVersion.current; };
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
    setParams((previous) => { const next = new URLSearchParams(previous); next.delete("tab"); return next; });
  };

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="My Bookings" text="Track and manage your reservations." />

        <div className="tabs">
          {STATUS_TABS.map((value) => (
            <button key={value} className={!waitlisted && status === value ? "active" : ""}
              aria-pressed={!waitlisted && status === value} onClick={() => changeStatus(value)}>
              {value ? labelize(value) : "ALL"}
            </button>
          ))}
          <button className={waitlisted ? "active" : ""} aria-pressed={waitlisted}
            onClick={() => setParams({ tab: "waitlisted" })}>WAITLISTED</button>
        </div>

        {waitlisted ? <WaitlistedBookings /> : <>
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
        </>}
      </div>
    </Layout>
  );
}
