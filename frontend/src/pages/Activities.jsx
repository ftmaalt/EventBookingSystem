import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";

import { activitiesApi } from "../api";
import Layout from "../components/Layout";
import ActivityCard from "../components/ActivityCard";
import { ActivitySkeletons, EmptyState, Pagination, SectionHeader } from "../components/ui";
import { getError } from "../utils/format";
import { useLookups } from "../utils/useLookups";

const PAGE_SIZE = 9;
const SEARCH_FETCH_SIZE = 100;

export default function Activities() {
  const [params] = useSearchParams();
  const { categories, locations, categoryById, locationById } = useLookups();

  const [activities, setActivities] = useState([]);
  const [categoryId, setCategoryId] = useState(params.get("category") || "");
  const [locationId, setLocationId] = useState("");
  const [search, setSearch] = useState(params.get("q") || "");
  const [query, setQuery] = useState((params.get("q") || "").trim().toLowerCase());
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Navigation searches can change the URL while this page is already mounted.
  useEffect(() => {
    const nextSearch = params.get("q") || "";
    setSearch(nextSearch);
    setQuery(nextSearch.trim().toLowerCase());
    setCategoryId(params.get("category") || "");
    setPage(0);
  }, [params]);

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
          setError(getError(err));
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
        <SectionHeader eyebrow="A LITTLE DISCOVERY GOES A LONG WAY" title="Find your next experience." text="Make time for something out of the ordinary." />

        <div className="filters">
          <label className="filter-field"><span>What inspires you?</span><input type="search" placeholder="Search activities..." value={search} onChange={(event) => setSearch(event.target.value)} /></label>
          <label className="filter-field"><span>Your kind of experience</span>
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
          </label>
          <label className="filter-field"><span>Somewhere to go</span>
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
          </label>
        </div>

        {error && <div className="error-box">{error}</div>}

        {loading ? (
          <ActivitySkeletons count={6} />
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
