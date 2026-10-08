import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { activitiesApi } from "../api";
import Layout from "../components/Layout";
import { EmptyState, Loading, Pagination, SectionHeader, StatusPill } from "../components/ui";
import { formatMoney } from "../utils/format";
import { useLookups } from "../utils/useLookups";

const PAGE_SIZE = 9;
const SEARCH_FETCH_SIZE = 100;

export function ActivityCard({ activity, categoryName, locationName }) {
  return (
    <div className="activity-card">
      <div className="activity-image">✦</div>
      <div className="activity-content">
        <StatusPill status={activity.status} />
        <h3>{activity.title}</h3>
        {(categoryName || locationName) && <small className="muted">{[categoryName, locationName].filter(Boolean).join(" · ")}</small>}
        <p>{activity.description}</p>
        <div className="activity-footer">
          <strong>{formatMoney(activity.pricePerPerson)}</strong>
          <Link className="small-button" to={`/activities/${activity.activity_id}`}>
            View
          </Link>
        </div>
      </div>
    </div>
  );
}

export default function Activities() {
  const { categories, locations, categoryById, locationById } = useLookups();

  const [activities, setActivities] = useState([]);
  const [categoryId, setCategoryId] = useState("");
  const [locationId, setLocationId] = useState("");
  const [search, setSearch] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Wait for the user to stop typing before searching.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(search.trim().toLowerCase());
      setPage(0);
    }, 300);
    return () => clearTimeout(timer);
  }, [search]);

  useEffect(() => {
    let cancelled = false;
    // The API only filters by category and location, so a text search loads a
    // larger batch and filters it here.
    const searching = query !== "";

    setLoading(true);
    activitiesApi
      .getAll({
        page: searching ? 0 : page,
        size: searching ? SEARCH_FETCH_SIZE : PAGE_SIZE,
        sort: "id,desc",
        categoryId: categoryId || undefined,
        locationId: locationId || undefined,
      })
      .then((response) => {
        if (cancelled) {
          return;
        }
        const items = (response.data.content || []).filter((item) => item.status === "ACTIVE");
        setActivities(
          searching
            ? items.filter(
                (item) =>
                  item.title?.toLowerCase().includes(query) || item.description?.toLowerCase().includes(query)
              )
            : items
        );
        setTotalPages(searching ? 0 : response.data.totalPages || 0);
        setError("");
      })
      .catch((err) => {
        if (!cancelled) {
          setActivities([]);
          setError(err.userMessage);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [page, categoryId, locationId, query]);

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="Explore Activities" text="Find something you want to experience." />

        <div className="filters">
          <input placeholder="Search activities..." value={search} onChange={(event) => setSearch(event.target.value)} />
          <select
            value={categoryId}
            onChange={(event) => {
              setCategoryId(event.target.value);
              setPage(0);
            }}
          >
            <option value="">All Categories</option>
            {categories.map((category) => (
              <option key={category.category_id} value={category.category_id}>
                {category.category_name}
              </option>
            ))}
          </select>
          <select
            value={locationId}
            onChange={(event) => {
              setLocationId(event.target.value);
              setPage(0);
            }}
          >
            <option value="">All Locations</option>
            {locations.map((location) => (
              <option key={location.id} value={location.id}>
                {location.name}
              </option>
            ))}
          </select>
        </div>

        {error && <div className="error-box">{error}</div>}

        {loading ? (
          <Loading />
        ) : activities.length === 0 ? (
          <EmptyState title="No activities found" text="Try changing your filters." />
        ) : (
          <div className="activity-grid">
            {activities.map((activity) => (
              <ActivityCard
                key={activity.activity_id}
                activity={activity}
                categoryName={categoryById(activity.categoryId)?.category_name}
                locationName={locationById(activity.locationId)?.name}
              />
            ))}
          </div>
        )}

        <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      </div>
    </Layout>
  );
}
