import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { activitiesApi, locationsApi, providerApi, sessionsApi } from "../api";
import { useAuth } from "../auth";
import Layout from "../components/Layout";
import { EmptyState, Feedback, Input, Loading, SectionHeader, Select, StatusPill, TextArea } from "../components/ui";
import ViolationForm from "../components/ViolationForm";
import { formatDate, formatMoney, getError, toInputDateTime } from "../utils/format";
import { useLookups } from "../utils/useLookups";

const TABS = [
  ["activities", "My Activities"],
  ["locations", "Locations"],
  ["profile", "Business Profile"],
  ["violations", "Violations"],
];

const EMPTY_ACTIVITY = { title: "", description: "", pricePerPerson: "", durationMinutes: "", categoryId: "", locationId: "" };
const EMPTY_SESSION = { startTime: "", endTime: "", capacity: 10 };
const EMPTY_LOCATION = { name: "", address: "", city: "" };

/** Create/edit form for an activity. */
function ActivityForm({ editing, categories, locations, onSaved, onCancel }) {
  const [form, setForm] = useState(
    editing
      ? {
          title: editing.title,
          description: editing.description,
          pricePerPerson: editing.pricePerPerson,
          durationMinutes: editing.durationMinutes,
          categoryId: editing.categoryId ?? "",
          locationId: editing.locationId ?? "",
        }
      : EMPTY_ACTIVITY
  );
  const [error, setError] = useState("");

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setError("");

    if (!form.title.trim() || !form.description.trim()) {
      setError("Title and description are required.");
      return;
    }
    if (!(Number(form.pricePerPerson) > 0)) {
      setError("Price must be greater than zero.");
      return;
    }
    if (!(Number(form.durationMinutes) >= 1)) {
      setError("Duration must be at least 1 minute.");
      return;
    }
    if (!form.categoryId || !form.locationId) {
      setError("Please choose a category and a location.");
      return;
    }

    const payload = {
      title: form.title.trim(),
      description: form.description.trim(),
      pricePerPerson: Number(form.pricePerPerson),
      durationMinutes: Number(form.durationMinutes),
      categoryId: Number(form.categoryId),
      locationId: Number(form.locationId),
    };

    try {
      const response = editing ? await activitiesApi.update(editing.activity_id, payload) : await activitiesApi.create(payload);
      onSaved(response.data, editing ? "Activity updated." : "Activity created. You can now add sessions to it.");
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <form className="form-card" onSubmit={submit}>
      <h2>{editing ? "Edit activity" : "Create activity"}</h2>
      <Feedback error={error} />

      <Input label="Title" value={form.title} onChange={(value) => update("title", value)} />
      <TextArea label="Description" value={form.description} onChange={(value) => update("description", value)} />
      <Input label="Price per person (BHD)" type="number" min="0" step="0.5" value={form.pricePerPerson} onChange={(value) => update("pricePerPerson", value)} />
      <Input label="Duration in minutes" type="number" min="1" value={form.durationMinutes} onChange={(value) => update("durationMinutes", value)} />
      <Select
        label="Category"
        value={form.categoryId}
        onChange={(value) => update("categoryId", value)}
        options={categories.map((item) => [item.category_id, item.category_name])}
      />
      <Select
        label="Location"
        value={form.locationId}
        onChange={(value) => update("locationId", value)}
        options={locations.map((item) => [item.id, `${item.name} (${item.city})`])}
      />

      <div className="button-row">
        <button className="primary-button">{editing ? "Save changes" : "Create activity"}</button>
        {editing && (
          <button type="button" className="secondary-button" onClick={onCancel}>
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}

/** Lists and manages the sessions of one activity. */
function SessionManager({ activity }) {
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [form, setForm] = useState(EMPTY_SESSION);
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      const response = await sessionsApi.getByActivity(activity.activity_id);
      setSessions([...(response.data || [])].sort((a, b) => a.startTime.localeCompare(b.startTime)));
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  }, [activity.activity_id]);

  useEffect(() => {
    load();
  }, [load]);

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const resetForm = () => {
    setForm(EMPTY_SESSION);
    setEditingId(null);
  };

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.startTime || !form.endTime) {
      setError("Start and end time are required.");
      return;
    }
    if (new Date(form.endTime) <= new Date(form.startTime)) {
      setError("The session must end after it starts.");
      return;
    }
    if (new Date(form.startTime) <= new Date()) {
      setError("The session must start in the future.");
      return;
    }
    if (!(Number(form.capacity) >= 1)) {
      setError("Capacity must be at least 1.");
      return;
    }

    const payload = {
      activity_id: activity.activity_id,
      startTime: form.startTime,
      endTime: form.endTime,
      capacity: Number(form.capacity),
    };

    try {
      if (editingId) {
        await sessionsApi.update(editingId, payload);
        setMessage("Session updated.");
      } else {
        await sessionsApi.create(payload);
        setMessage("Session created.");
      }
      resetForm();
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  const startEditing = (session) => {
    setEditingId(session.id);
    setForm({
      startTime: toInputDateTime(session.startTime),
      endTime: toInputDateTime(session.endTime),
      capacity: session.capacity,
    });
    setMessage("");
    setError("");
  };

  const remove = async (session) => {
    if (!window.confirm("Delete this session?")) {
      return;
    }
    setMessage("");
    setError("");
    try {
      await sessionsApi.remove(session.id);
      setMessage("Session deleted.");
      if (editingId === session.id) {
        resetForm();
      }
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  const refreshStatus = async (session) => {
    setMessage("");
    setError("");
    try {
      await sessionsApi.refreshStatus(session.id);
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <div className="form-card">
      <h2>Sessions for “{activity.title}”</h2>
      <Feedback error={error} message={message} />

      {loading ? (
        <Loading />
      ) : sessions.length === 0 ? (
        <EmptyState title="No sessions yet" text="Create the first session below." />
      ) : (
        <div className="session-list">
          {sessions.map((session) => (
            <div className="session-card" key={session.id}>
              <div>
                <strong>{formatDate(session.startTime)}</strong>
                <span>Until {formatDate(session.endTime)}</span>
                <span>
                  {session.spotsLeft} of {session.capacity} spots left · Session #{session.id}
                </span>
              </div>
              <div className="session-right">
                <StatusPill status={session.status} />
                <button className="small-button" onClick={() => refreshStatus(session)}>
                  Refresh status
                </button>
                <button className="small-button" onClick={() => startEditing(session)}>
                  Edit
                </button>
                <button className="danger-button small" onClick={() => remove(session)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <form onSubmit={submit}>
        <h3>{editingId ? `Edit session #${editingId}` : "Add a session"}</h3>
        <Input label="Start" type="datetime-local" value={form.startTime} onChange={(value) => update("startTime", value)} />
        <Input label="End" type="datetime-local" value={form.endTime} onChange={(value) => update("endTime", value)} />
        <Input label="Capacity" type="number" min="1" value={form.capacity} onChange={(value) => update("capacity", value)} />
        <div className="button-row">
          <button className="primary-button">{editingId ? "Save session" : "Create session"}</button>
          {editingId && (
            <button type="button" className="secondary-button" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>
    </div>
  );
}

function ActivitiesTab() {
  const { user } = useAuth();
  const { categories, locations, categoryById, locationById } = useLookups();

  const [activities, setActivities] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null);
  const [selectedId, setSelectedId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      // The API has no "my activities" endpoint, so fetch a large page and keep the provider's own.
      const response = await activitiesApi.getAll({ page: 0, size: 200, sort: "id,desc" });
      setActivities((response.data.content || []).filter((item) => item.providerId === user?.id));
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    if (user) {
      load();
    }
  }, [user, load]);

  const handleSaved = async (saved, text) => {
    setMessage(text);
    setError("");
    setEditing(null);
    setSelectedId(saved.activity_id);
    await load();
  };

  const remove = async (activity) => {
    if (!window.confirm(`Delete “${activity.title}”? This cannot be undone.`)) {
      return;
    }
    setMessage("");
    setError("");
    try {
      await activitiesApi.remove(activity.activity_id);
      setMessage("Activity deleted.");
      if (selectedId === activity.activity_id) {
        setSelectedId(null);
      }
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  const selected = activities.find((item) => item.activity_id === selectedId);

  return (
    <>
      <Feedback error={error} message={message} />

      <div className="two-column">
        <ActivityForm
          key={editing?.activity_id ?? "new"}
          editing={editing}
          categories={categories}
          locations={locations}
          onSaved={handleSaved}
          onCancel={() => setEditing(null)}
        />

        <div className="form-card">
          <h2>Your activities</h2>
          {loading ? (
            <Loading />
          ) : activities.length === 0 ? (
            <EmptyState title="No activities yet" text="Create your first activity to get started." />
          ) : (
            <div className="application-list">
              {activities.map((activity) => (
                <div className="application" key={activity.activity_id}>
                  <div>
                    <strong>{activity.title}</strong>
                    <span>
                      {formatMoney(activity.pricePerPerson)} · {activity.durationMinutes} min
                    </span>
                    <span>
                      {[categoryById(activity.categoryId)?.category_name, locationById(activity.locationId)?.name]
                        .filter(Boolean)
                        .join(" · ")}
                    </span>
                    <Link to={`/activities/${activity.activity_id}`}>View public page</Link>
                  </div>
                  <div className="button-row">
                    <button className="small-button" onClick={() => setSelectedId(activity.activity_id)}>
                      Sessions
                    </button>
                    <button
                      className="small-button"
                      onClick={() => {
                        setEditing(activity);
                        window.scrollTo({ top: 0, behavior: "smooth" });
                      }}
                    >
                      Edit
                    </button>
                    <button className="danger-button small" onClick={() => remove(activity)}>
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {selected && <SessionManager key={selected.activity_id} activity={selected} />}
    </>
  );
}

function LocationsTab() {
  const { locations, reload } = useLookups();
  const [form, setForm] = useState(EMPTY_LOCATION);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const update = (field, value) => setForm((previous) => ({ ...previous, [field]: value }));

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.name.trim() || !form.address.trim() || !form.city.trim()) {
      setError("Name, address and city are required.");
      return;
    }

    try {
      await locationsApi.create({ name: form.name.trim(), address: form.address.trim(), city: form.city.trim() });
      setMessage("Location created.");
      setForm(EMPTY_LOCATION);
      await reload();
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <div className="two-column">
      <form className="form-card" onSubmit={submit}>
        <h2>Add a location</h2>
        <p className="muted">The map coordinates are looked up automatically from the address.</p>
        <Feedback error={error} message={message} />
        <Input label="Name" value={form.name} onChange={(value) => update("name", value)} />
        <Input label="Address" value={form.address} onChange={(value) => update("address", value)} />
        <Input label="City" value={form.city} onChange={(value) => update("city", value)} />
        <button className="primary-button">Create location</button>
      </form>

      <div className="form-card">
        <h2>All locations</h2>
        {locations.length === 0 ? (
          <EmptyState title="No locations" text="Locations you add will appear here." />
        ) : (
          <div className="application-list">
            {locations.map((location) => (
              <div className="application" key={location.id}>
                <div>
                  <strong>{location.name}</strong>
                  <span>
                    {location.address}, {location.city}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ProfileTab() {
  const [profile, setProfile] = useState(null);
  const [form, setForm] = useState({ phone: "", description: "" });
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  useEffect(() => {
    providerApi
      .getMe()
      .then((response) => {
        setProfile(response.data);
        setForm({ phone: response.data.phone || "", description: response.data.description || "" });
      })
      .catch((err) => setError(getError(err)))
      .finally(() => setLoading(false));
  }, []);

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!/^\d{8}$/.test(form.phone)) {
      setError("Phone number must be exactly 8 digits.");
      return;
    }

    try {
      const response = await providerApi.updateMe(form);
      setProfile(response.data);
      setMessage("Business profile updated.");
    } catch (err) {
      setError(getError(err));
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <form className="form-card" onSubmit={submit}>
      <h2>{profile?.businessName || "Business profile"}</h2>
      <Feedback error={error} message={message} />
      {profile && (
        <>
          <Input
            label="Business phone"
            type="tel"
            inputMode="numeric"
            maxLength={8}
            value={form.phone}
            onChange={(value) => setForm({ ...form, phone: value.replace(/\D/g, "") })}
          />
          <TextArea label="Description" value={form.description} onChange={(value) => setForm({ ...form, description: value })} />
          <button className="primary-button">Save profile</button>
        </>
      )}
    </form>
  );
}

export default function Provider() {
  const [tab, setTab] = useState("activities");

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="Provider Dashboard" text="Manage your activities, sessions and business details." />

        <div className="tabs">
          {TABS.map(([key, label]) => (
            <button key={key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </div>

        {tab === "activities" && <ActivitiesTab />}
        {tab === "locations" && <LocationsTab />}
        {tab === "profile" && <ProfileTab />}
        {tab === "violations" && <ViolationForm />}
      </div>
    </Layout>
  );
}
