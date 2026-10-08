import { useCallback, useEffect, useState } from "react";

import { adminApi, applicationsApi, categoriesApi } from "../api";
import Layout from "../components/Layout";
import { EmptyState, Feedback, Input, Loading, Pagination, SectionHeader, Select, StatusPill, TextArea } from "../components/ui";
import ViolationForm from "../components/ViolationForm";
import { formatDate, getError } from "../utils/format";

const TABS = [
  ["applications", "Provider Applications"],
  ["users", "Users"],
  ["categories", "Categories"],
  ["violations", "Violations"],
  ["audit", "Audit Log"],
];

const LOG_PAGE_SIZE = 15;

function ApplicationsTab() {
  const [applications, setApplications] = useState([]);
  const [notes, setNotes] = useState({});
  const [loading, setLoading] = useState(true);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      const response = await applicationsApi.getAll();
      // Pending applications first, then newest.
      setApplications(
        [...(response.data || [])].sort(
          (a, b) =>
            Number(b.status === "PENDING") - Number(a.status === "PENDING") ||
            String(b.createdAt).localeCompare(String(a.createdAt))
        )
      );
    } catch (err) {
      setError(getError(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const review = async (application, action) => {
    setMessage("");
    setError("");
    try {
      await applicationsApi.review(application.application_id, action, notes[application.application_id] || "");
      setMessage(`Application from ${application.businessName} ${action === "approve" ? "approved" : "rejected"}.`);
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  if (loading) {
    return <Loading />;
  }

  return (
    <div className="form-card">
      <h2>Provider applications</h2>
      <Feedback error={error} message={message} />

      {applications.length === 0 ? (
        <EmptyState title="No applications" text="Provider applications will appear here." />
      ) : (
        <div className="application-list">
          {applications.map((application) => (
            <div className="application" key={application.application_id}>
              <div>
                <strong>{application.businessName}</strong>
                <span>
                  {application.contactName} · {application.city}
                </span>
                <span>Proposes: {application.proposedActivities}</span>
                <span>Submitted {formatDate(application.createdAt)}</span>
                {application.reviewNote && <span>Note: {application.reviewNote}</span>}
              </div>

              {application.status === "PENDING" ? (
                <div>
                  <TextArea
                    label="Review note (optional)"
                    value={notes[application.application_id] || ""}
                    onChange={(value) => setNotes({ ...notes, [application.application_id]: value })}
                  />
                  <div className="button-row">
                    <button className="small-button" onClick={() => review(application, "approve")}>
                      Approve
                    </button>
                    <button className="danger-button small" onClick={() => review(application, "reject")}>
                      Reject
                    </button>
                  </div>
                </div>
              ) : (
                <StatusPill status={application.status} />
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function UsersTab() {
  const [userId, setUserId] = useState("");
  const [role, setRole] = useState("USER");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const run = async (action, successText) => {
    setMessage("");
    setError("");
    if (!(Number(userId) > 0)) {
      setError("Enter a valid user ID.");
      return;
    }
    try {
      const response = await action(Number(userId));
      setMessage(response.data?.message || successText);
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <div className="form-card">
      <h2>User management</h2>
      <p className="muted">Change a user's role or deactivate their account (the account is kept, only its status changes).</p>
      <Feedback error={error} message={message} />

      <Input label="User ID" type="number" min="1" value={userId} onChange={setUserId} />
      <Select
        label="Role"
        value={role}
        onChange={setRole}
        options={[
          ["USER", "USER"],
          ["PROVIDER", "PROVIDER"],
          ["ADMIN", "ADMIN"],
        ]}
      />

      <div className="button-row">
        <button className="primary-button" onClick={() => run((id) => adminApi.updateRole(id, role), "User role updated.")}>
          Update role
        </button>
        <button
          className="danger-button"
          onClick={() => window.confirm("Deactivate this user?") && run(adminApi.deactivateUser, "User deactivated.")}
        >
          Deactivate user
        </button>
      </div>
    </div>
  );
}

function CategoriesTab() {
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState({ category_name: "", description: "" });
  const [editingId, setEditingId] = useState(null);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    try {
      setCategories((await categoriesApi.getAll()).data);
    } catch (err) {
      setError(getError(err));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const resetForm = () => {
    setForm({ category_name: "", description: "" });
    setEditingId(null);
  };

  const submit = async (event) => {
    event.preventDefault();
    setMessage("");
    setError("");

    if (!form.category_name.trim()) {
      setError("Category name is required.");
      return;
    }

    try {
      if (editingId) {
        await categoriesApi.update(editingId, form);
        setMessage("Category updated.");
      } else {
        await categoriesApi.create(form);
        setMessage("Category created.");
      }
      resetForm();
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  const remove = async (category) => {
    if (!window.confirm(`Delete “${category.category_name}”?`)) {
      return;
    }
    setMessage("");
    setError("");
    try {
      await categoriesApi.remove(category.category_id);
      setMessage("Category deleted.");
      await load();
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <div className="two-column">
      <form className="form-card" onSubmit={submit}>
        <h2>{editingId ? "Edit category" : "Add category"}</h2>
        <Feedback error={error} message={message} />
        <Input label="Name" value={form.category_name} onChange={(value) => setForm({ ...form, category_name: value })} />
        <TextArea label="Description" value={form.description} onChange={(value) => setForm({ ...form, description: value })} />
        <div className="button-row">
          <button className="primary-button">{editingId ? "Save changes" : "Create category"}</button>
          {editingId && (
            <button type="button" className="secondary-button" onClick={resetForm}>
              Cancel
            </button>
          )}
        </div>
      </form>

      <div className="form-card">
        <h2>Categories</h2>
        {categories.length === 0 ? (
          <EmptyState title="No categories" text="Add the first category." />
        ) : (
          <div className="application-list">
            {categories.map((category) => (
              <div className="application" key={category.category_id}>
                <div>
                  <strong>{category.category_name}</strong>
                  <span>{category.description}</span>
                </div>
                <div className="button-row">
                  <button
                    className="small-button"
                    onClick={() => {
                      setEditingId(category.category_id);
                      setForm({ category_name: category.category_name, description: category.description || "" });
                    }}
                  >
                    Edit
                  </button>
                  <button className="danger-button small" onClick={() => remove(category)}>
                    Delete
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function AuditTab() {
  const [logs, setLogs] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    adminApi
      .getAuditLogs({ page, size: LOG_PAGE_SIZE, sort: "timestamp,desc" })
      .then((response) => {
        if (!cancelled) {
          setLogs(response.data.content || []);
          setTotalPages(response.data.totalPages || 0);
          setError("");
        }
      })
      .catch((err) => !cancelled && setError(getError(err)))
      .finally(() => !cancelled && setLoading(false));

    return () => {
      cancelled = true;
    };
  }, [page]);

  return (
    <div className="form-card">
      <h2>Audit log</h2>
      <Feedback error={error} />

      {loading ? (
        <Loading />
      ) : logs.length === 0 ? (
        <EmptyState title="No audit logs" text="Audit events will appear here." />
      ) : (
        <div className="table-container">
          <table>
            <thead>
              <tr>
                <th>Time</th>
                <th>Action</th>
                <th>Performed by</th>
                <th>Target</th>
                <th>Details</th>
              </tr>
            </thead>
            <tbody>
              {logs.map((log) => (
                <tr key={log.id}>
                  <td>{formatDate(log.timestamp)}</td>
                  <td>{log.action}</td>
                  <td>{log.performedBy}</td>
                  <td>
                    {log.targetEntity}
                    {log.targetId != null && ` #${log.targetId}`}
                  </td>
                  <td>{log.details}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
    </div>
  );
}

export default function Admin() {
  const [tab, setTab] = useState("applications");

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="Admin Panel" text="Manage provider applications, users, categories and the audit log." />

        <div className="tabs">
          {TABS.map(([key, label]) => (
            <button key={key} className={tab === key ? "active" : ""} onClick={() => setTab(key)}>
              {label}
            </button>
          ))}
        </div>

        {tab === "applications" && <ApplicationsTab />}
        {tab === "users" && <UsersTab />}
        {tab === "categories" && <CategoriesTab />}
        {tab === "violations" && <ViolationForm />}
        {tab === "audit" && <AuditTab />}
      </div>
    </Layout>
  );
}
