import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";

import { activitiesApi, sessionsApi } from "../api";
import { useAuth } from "../auth";
import Layout from "../components/Layout";
import { EmptyState, Loading, SectionHeader, StatusPill } from "../components/ui";
import { formatDate, formatMoney, getError } from "../utils/format";
import { isBookable } from "../utils/sessions";
import { useLookups } from "../utils/useLookups";

function SessionCard({ session, loggedIn }) {
  const bookable = isBookable(session);

  return (
    <div className="session-card">
      <div>
        <strong>{formatDate(session.startTime)}</strong>
        <span>Until {formatDate(session.endTime)}</span>
        <span>
          {session.spotsLeft} of {session.capacity} spots remaining
        </span>
      </div>
      <div className="session-right">
        <StatusPill status={session.status} />
        {!bookable ? (
          <button className="secondary-button" disabled>
            Unavailable
          </button>
        ) : (
          <Link
            className="primary-button"
            to={loggedIn ? `/book/${session.id}` : "/login"}
            state={loggedIn ? undefined : { from: `/book/${session.id}` }}
          >
            {loggedIn ? "Book" : "Login to book"}
          </Link>
        )}
      </div>
    </div>
  );
}

export default function ActivityDetails() {
  const { id } = useParams();
  const { token } = useAuth();
  const { categoryById, locationById } = useLookups();

  const [activity, setActivity] = useState(null);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

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

  return (
    <Layout>
      <div className="page">
        <Link className="back-link" to="/activities">
          ← Back to activities
        </Link>

        <div className="activity-detail">
          <div className="detail-image">✦</div>
          <div>
            <StatusPill status={activity.status} />
            <h1>{activity.title}</h1>
            <p className="detail-description">{activity.description}</p>
            <div className="detail-price">
              {formatMoney(activity.pricePerPerson)}
              <small>per person</small>
            </div>
            <p>
              Duration: <strong>{activity.durationMinutes} minutes</strong>
            </p>
            {category && (
              <p>
                Category: <strong>{category.category_name}</strong>
              </p>
            )}
            {location && (
              <p>
                Location:{" "}
                <strong>
                  {location.name}, {location.address}, {location.city}
                </strong>
                {location.latitude != null && location.longitude != null && (
                  <>
                    {" · "}
                    <a
                      href={`https://www.openstreetmap.org/?mlat=${location.latitude}&mlon=${location.longitude}#map=16/${location.latitude}/${location.longitude}`}
                      target="_blank"
                      rel="noreferrer"
                    >
                      View on map
                    </a>
                  </>
                )}
              </p>
            )}
          </div>
        </div>

        <SectionHeader title="Available Sessions" text="Choose a session to continue with your booking." />
        <div className="session-list">
          {sessions.length === 0 ? (
            <EmptyState title="No sessions" text="There are currently no sessions for this activity." />
          ) : (
            sessions.map((session) => <SessionCard key={session.id} session={session} loggedIn={Boolean(token)} />)
          )}
        </div>
      </div>
    </Layout>
  );
}
