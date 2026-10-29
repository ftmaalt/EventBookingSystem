import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { activitiesApi, sessionsApi } from "../api";
import Layout from "../components/Layout";
import LocationMap from "../components/LocationMap";
import SessionCard from "../components/SessionCard";
import { EmptyState, Loading, SectionHeader, StatusPill } from "../components/ui";
import { formatDate, formatMoney, getError } from "../utils/format";
import { isBookable } from "../utils/sessions";
import { useLookups } from "../utils/useLookups";

export default function ActivityDetails() {
  const { id } = useParams();
  const { categoryById, locationById } = useLookups();

  const [activity, setActivity] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const refreshSessions = () => {
    sessionsApi.getByActivity(id).then(({ data }) => {
      setSessions([...(data || [])].sort((a, b) => a.startTime.localeCompare(b.startTime)));
    }).catch(() => {}); // A waitlist conflict remains visible even if refreshing sessions fails.
  };

  useEffect(() => {
    let cancelled = false;
    Promise.all([activitiesApi.getById(id), sessionsApi.getByActivity(id)])
      .then(([activityResponse, sessionsResponse]) => {
        if (cancelled) {
          return;
        }
        setActivity(activityResponse.data);
        setSessions([...(sessionsResponse.data || [])].sort((a, b) => a.startTime.localeCompare(b.startTime)));
      })
      .catch((err) => !cancelled && setError(getError(err)))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [id]);

  if (loading) {
    return (
      <Layout>
        <Loading />
      </Layout>
    );
  }

  if (error || !activity) {
    return (
      <Layout>
        <div className="page">
          <Link className="back-link" to="/activities">
            ← Back to activities
          </Link>
          <div className="error-box">{error || "Activity not found."}</div>
        </div>
      </Layout>
    );
  }

  const category = categoryById(activity.categoryId);
  const location = locationById(activity.locationId);
  const nextSession = sessions.find(isBookable);

  return (
    <Layout>
      <div className="page">
        <Link className="back-link" to="/activities">
          ← Back to activities
        </Link>

        <header className="detail-heading">
          <div className="activity-labels">
            {category && <span className="category-badge">{category.category_name}</span>}
            <StatusPill status={activity.status} />
          </div>
          <h1>{activity.title}</h1>
          <p className="muted">A little time out. A new story to tell.</p>
        </header>

        <div className="detail-image detail-cover" aria-hidden="true"><span>✦</span><small>BOOK · EXPLORE · EXPERIENCE</small></div>

        <div className="detail-layout">
          <div className="detail-main">
            <span className="eyebrow">THE EXPERIENCE</span>
            <h2>A moment to make your own.</h2>
            <p className="detail-description">{activity.description}</p>
            <dl className="detail-facts">
              <div><dt>Make time for</dt><dd>{activity.durationMinutes} minutes</dd></div>
              {category && <div><dt>Your kind of moment</dt><dd>{category.category_name}</dd></div>}
              {location && <div><dt>Meet us here</dt><dd><strong>{location.name}</strong><br />{location.address}, {location.city}</dd></div>}
            </dl>
            <h2 className="location-heading">A place to be.</h2>
            <LocationMap location={location} />
          </div>

          <aside className="detail-booking-panel" aria-label="Plan your booking">
            <span className="eyebrow">MAKE IT A PLAN</span>
            <h2>Your next good moment.</h2>
            <div className="detail-price">
              {formatMoney(activity.pricePerPerson)}
              <small>per person</small>
            </div>
            {nextSession ? <div className="next-session"><small>NEXT BOOKABLE SESSION</small>
              <strong>{formatDate(nextSession.startTime)}</strong>
              <span>{nextSession.spotsLeft} spots remaining</span></div>
              : <p className="muted">Check the sessions below for dates and availability.</p>}
            <a className="primary-button full" href="#available-sessions">{nextSession ? "Choose a session" : "View sessions"}<span aria-hidden="true">↓</span></a>
            <p className="booking-note">Choose your date and participants before confirming your booking.</p>
          </aside>
        </div>

        <div id="available-sessions" className="sessions-heading"><SectionHeader eyebrow="SAVE A LITTLE TIME FOR YOURSELF" title="Available Sessions" text="Choose a session to continue with your booking." /></div>
        <div className="session-list">
          {sessions.length === 0 ? (
            <EmptyState title="No sessions" text="There are currently no sessions for this activity." />
          ) : (
            sessions.map((session) => <SessionCard key={session.id} session={session}
              activityActive={activity.status === "ACTIVE"} returnTo={`/activities/${id}`}
              onSessionRefresh={refreshSessions} />)
          )}
        </div>
      </div>
    </Layout>
  );
}
