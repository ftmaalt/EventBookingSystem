import {
  useEffect,
  useMemo,
  useState,
} from "react";

import {
  Link,
  Navigate,
  Route,
  Routes,
  useNavigate,
  useParams,
} from "react-router-dom";

import {
  activitiesApi,
  applicationsApi,
  bookingsApi,
  categoriesApi,
  locationsApi,
  sessionsApi,
  usersApi,
  providerApi,
  adminApi,
  authApi,
} from "./api";

import { useAuth } from "./auth";

/* ==================================================
   HELPERS
================================================== */

function formatMoney(value) {
  if (value === null || value === undefined) {
    return "—";
  }

  return `${Number(value).toFixed(2)} BHD`;
}

function formatDate(value) {
  if (!value) {
    return "—";
  }

  try {
    return new Date(value).toLocaleString(
        [],
        {
          dateStyle: "medium",
          timeStyle: "short",
        }
    );
  } catch {
    return value;
  }
}

function getError(error) {
  return (
      error?.userMessage ||
      error?.response?.data?.message ||
      error?.message ||
      "Something went wrong."
  );
}

/* ==================================================
   LAYOUT
================================================== */

function Layout({ children }) {
  const {
    token,
    role,
    user,
    logout,
  } = useAuth();

  const navigate = useNavigate();

  const [mobileOpen, setMobileOpen] =
      useState(false);

  const isAdmin =
      role === "ADMIN";

  const isProvider =
      role === "PROVIDER";

  const links = [];

  if (token) {
    links.push(
        {
          to: "/dashboard",
          label: "Dashboard",
        },
        {
          to: "/activities",
          label: "Explore",
        },
        {
          to: "/bookings",
          label: "My Bookings",
        },
        {
          to: "/profile",
          label: "Profile",
        }
    );

    if (isProvider) {
      links.push({
        to: "/provider",
        label: "Provider",
      });
    }

    if (isAdmin) {
      links.push({
        to: "/admin",
        label: "Admin",
      });
    }
  }

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  return (
      <div className="app-shell">

        <header className="navbar">

          <Link
              className="logo"
              to={token ? "/dashboard" : "/"}
          >
          <span className="logo-mark">
            BN
          </span>

            <span>
            Book<span>N</span>Go
          </span>
          </Link>

          <button
              className="mobile-menu"
              onClick={() =>
                  setMobileOpen(
                      !mobileOpen
                  )
              }
          >
            ☰
          </button>

          <nav
              className={
                mobileOpen
                    ? "navigation open"
                    : "navigation"
              }
          >

            {links.map((link) => (
                <Link
                    key={link.to}
                    to={link.to}
                    onClick={() =>
                        setMobileOpen(false)
                    }
                >
                  {link.label}
                </Link>
            ))}

            {!token && (
                <>
                  <Link to="/login">
                    Login
                  </Link>

                  <Link
                      className="nav-button"
                      to="/register"
                  >
                    Register
                  </Link>
                </>
            )}

            {token && (
                <button
                    className="logout-button"
                    onClick={handleLogout}
                >
                  Logout
                </button>
            )}
          </nav>
        </header>

        <main>
          {children}
        </main>

        <NotificationToasts />

        <footer>
          Book N Go · Activity Booking Platform
        </footer>
      </div>
  );
}

/* ==================================================
   TOASTS
================================================== */

function NotificationToasts() {
  const {
    notifications,
    setNotifications,
  } = useAuth();

  if (!notifications.length) {
    return null;
  }

  return (
      <div className="toast-container">

        {notifications
            .slice(0, 4)
            .map((notification) => (
                <div
                    className="toast"
                    key={notification.id}
                >
                  <div>
                    <strong>
                      Notification
                    </strong>

                    <p>
                      {notification.message}
                    </p>
                  </div>

                  <button
                      onClick={() =>
                          setNotifications(
                              (items) =>
                                  items.filter(
                                      (item) =>
                                          item.id !==
                                          notification.id
                                  )
                          )
                      }
                  >
                    ×
                  </button>
                </div>
            ))}
      </div>
  );
}

/* ==================================================
   PROTECTED ROUTE
================================================== */

function Protected({
                     children,
                     roles,
                   }) {
  const {
    token,
    role,
    loading,
  } = useAuth();

  if (loading) {
    return (
        <Loading />
    );
  }

  if (!token) {
    return (
        <Navigate
            to="/login"
            replace
        />
    );
  }

  if (
      roles &&
      !roles.includes(role)
  ) {
    return (
        <Navigate
            to="/dashboard"
            replace
        />
    );
  }

  return children;
}

/* ==================================================
   LOADING
================================================== */

function Loading() {
  return (
      <div className="loading-page">
        <div className="spinner" />
        <p>Loading...</p>
      </div>
  );
}

/* ==================================================
   LANDING PAGE
================================================== */

function Landing() {
  return (
      <Layout>

        <section className="hero">

          <div className="hero-content">

          <span className="eyebrow">
            BOOK · EXPLORE · EXPERIENCE
          </span>

            <h1>
              Find your next
              <span> experience.</span>
            </h1>

            <p>
              Discover activities,
              explore available sessions
              and reserve your spot through
              one simple platform.
            </p>

            <div className="hero-actions">

              <Link
                  className="primary-button"
                  to="/register"
              >
                Get Started
              </Link>

              <Link
                  className="secondary-button"
                  to="/login"
              >
                I already have an account
              </Link>

            </div>
          </div>

          <div className="hero-card">

            <div className="hero-card-icon">
              ✦
            </div>

            <h3>
              Everything in one place
            </h3>

            <p>
              Activities, sessions,
              bookings and notifications
              connected directly to your
              Spring Boot backend.
            </p>

          </div>

        </section>

        <section className="section">

          <SectionHeader
              title="How it works"
              text="Test the complete booking workflow from beginning to end."
          />

          <div className="feature-grid">

            <Feature
                number="01"
                title="Explore"
                text="Browse activities and filter them by category and location."
            />

            <Feature
                number="02"
                title="Choose"
                text="Open an activity and view its available sessions."
            />

            <Feature
                number="03"
                title="Book"
                text="Select participants and booking type, then confirm."
            />

            <Feature
                number="04"
                title="Manage"
                text="Track your bookings and cancel them when necessary."
            />

          </div>

        </section>

      </Layout>
  );
}

/* ==================================================
   FEATURE
================================================== */

function Feature({
                   number,
                   title,
                   text,
                 }) {
  return (
      <div className="feature-card">

      <span>
        {number}
      </span>

        <h3>
          {title}
        </h3>

        <p>
          {text}
        </p>

      </div>
  );
}

/* ==================================================
   SECTION HEADER
================================================== */

function SectionHeader({
                         title,
                         text,
                       }) {
  return (
      <div className="section-header">

        <div>
          <h2>
            {title}
          </h2>

          <p>
            {text}
          </p>
        </div>

      </div>
  );
}

/* ==================================================
   LOGIN
================================================== */

function Login() {
  const navigate =
      useNavigate();

  const { login } =
      useAuth();

  const [email, setEmail] =
      useState("");

  const [password, setPassword] =
      useState("");

  const [loading, setLoading] =
      useState(false);

  const [error, setError] =
      useState("");

  const submit = async (event) => {
    event.preventDefault();

    setError("");

    if (!email || !password) {
      setError(
          "Email and password are required."
      );

      return;
    }

    setLoading(true);

    try {
      const result =
          await login({
            email,
            password,
          });

      if (
          result.role === "ADMIN"
      ) {
        navigate("/admin");
      } else {
        navigate("/dashboard");
      }

    } catch (error) {
      setError(
          getError(error)
      );
    } finally {
      setLoading(false);
    }
  };

  return (
      <Layout>

        <div className="auth-page">

          <form
              className="auth-card"
              onSubmit={submit}
          >

          <span className="eyebrow">
            BOOK N GO
          </span>

            <h1>
              Welcome back
            </h1>

            <p className="muted">
              Login to continue.
            </p>

            <Input
                label="Email"
                type="email"
                value={email}
                onChange={setEmail}
            />

            <Input
                label="Password"
                type="password"
                value={password}
                onChange={setPassword}
            />

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            <button
                className="primary-button full"
                disabled={loading}
            >
              {loading
                  ? "Logging in..."
                  : "Login"}
            </button>

            <p className="auth-switch">
              Don't have an account?

              {" "}

              <Link to="/register">
                Register
              </Link>
            </p>

          </form>

        </div>

      </Layout>
  );
}

/* ==================================================
   REGISTER
================================================== */

function Register() {
  const navigate =
      useNavigate();

  const [form, setForm] =
      useState({
        fullname: "",
        email: "",
        password: "",
        phone: "",
      });

  const [loading, setLoading] =
      useState(false);

  const [message, setMessage] =
      useState("");

  const [error, setError] =
      useState("");

  const update = (
      field,
      value
  ) => {
    setForm({
      ...form,
      [field]: value,
    });
  };

  const submit = async (event) => {
    event.preventDefault();

    setError("");
    setMessage("");

    if (
        !form.fullname ||
        !form.email ||
        !form.password ||
        !form.phone
    ) {
      setError(
          "Please complete all fields."
      );

      return;
    }

    if (
        !/^\S+@\S+\.\S+$/.test(
            form.email
        )
    ) {
      setError(
          "Please enter a valid email address."
      );

      return;
    }

    if (
        form.password.length < 8
    ) {
      setError(
          "Password must contain at least 8 characters."
      );

      return;
    }

    setLoading(true);

    try {
      await authApi.register(
          form
      );

      setMessage(
          "Registration successful. You can now login."
      );

      setTimeout(() => {
        navigate("/login");
      }, 1200);

    } catch (error) {
      setError(
          getError(error)
      );
    } finally {
      setLoading(false);
    }
  };

  return (
      <Layout>

        <div className="auth-page">

          <form
              className="auth-card"
              onSubmit={submit}
          >

          <span className="eyebrow">
            JOIN BOOK N GO
          </span>

            <h1>
              Create account
            </h1>

            <p className="muted">
              Create an account to start
              booking activities.
            </p>

            <Input
                label="Full name"
                value={form.fullname}
                onChange={(value) =>
                    update(
                        "fullname",
                        value
                    )
                }
            />

            <Input
                label="Email"
                type="email"
                value={form.email}
                onChange={(value) =>
                    update(
                        "email",
                        value
                    )
                }
            />

            <label className="field">
              <span>Phone</span>

              <input
                  type="tel"
                  inputMode="numeric"
                  maxLength={8}
                  placeholder="e.g. 36252994"
                  value={form.phone}
                  onChange={(event) => {
                    const value = event.target.value.replace(/\D/g, "");
                    update("phone", value);
                  }}
              />

              <small>
                Enter an 8-digit phone number
              </small>
            </label>

            <Input
                label="Password"
                type="password"
                value={form.password}
                onChange={(value) =>
                    update(
                        "password",
                        value
                    )
                }
            />

            <PasswordRules
                password={form.password}
            />

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            <button
                className="primary-button full"
                disabled={loading}
            >
              {loading
                  ? "Creating..."
                  : "Create Account"}
            </button>

            <p className="auth-switch">
              Already registered?

              {" "}

              <Link to="/login">
                Login
              </Link>
            </p>

          </form>

        </div>

      </Layout>
  );
}

/* ==================================================
   PASSWORD RULES
================================================== */

function PasswordRules({
                         password,
                       }) {
  const valid =
      password.length >= 8;

  return (
      <div
          className={
            valid
                ? "password-rules valid"
                : "password-rules"
          }
      >
        {valid ? "✓" : "○"} At least 8 characters
      </div>
  );
}

/* ==================================================
   DASHBOARD
================================================== */

function Dashboard() {
  const {
    user,
    role,
  } = useAuth();

  return (
      <Layout>

        <div className="page">

          <div className="dashboard-header">

            <div>

            <span className="eyebrow">
              DASHBOARD
            </span>

              <h1>
                Welcome,
                {" "}
                {user?.fullName ||
                    "there"}
              </h1>

              <p>
                Choose what you want
                to do next.
              </p>

            </div>

            <Link
                className="primary-button"
                to="/activities"
            >
              Explore Activities
            </Link>

          </div>

          <div className="stats-grid">

            <DashboardStat
                value="Explore"
                label="Activities"
            />

            <DashboardStat
                value="Book"
                label="A Session"
            />

            <DashboardStat
                value="Track"
                label="Your Bookings"
            />

            <DashboardStat
                value={role}
                label="Current Role"
            />

          </div>

          <div className="quick-grid">

            <QuickCard
                to="/activities"
                title="Explore Activities"
                text="Browse available activities and sessions."
            />

            <QuickCard
                to="/bookings"
                title="My Bookings"
                text="View your current and previous bookings."
            />

            <QuickCard
                to="/profile"
                title="Profile"
                text="Update your personal information and password."
            />

            {role === "PROVIDER" && (
                <QuickCard
                    to="/provider"
                    title="Provider"
                    text="Create activities and sessions."
                />
            )}

            {role === "ADMIN" && (
                <QuickCard
                    to="/admin"
                    title="Admin Panel"
                    text="Manage users, applications and audit logs."
                />
            )}

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   DASHBOARD STAT
================================================== */

function DashboardStat({
                         value,
                         label,
                       }) {
  return (
      <div className="stat-card">

        <strong>
          {value}
        </strong>

        <span>
        {label}
      </span>

      </div>
  );
}

/* ==================================================
   QUICK CARD
================================================== */

function QuickCard({
                     to,
                     title,
                     text,
                   }) {
  return (
      <Link
          to={to}
          className="quick-card"
      >

        <div>

          <h3>
            {title}
          </h3>

          <p>
            {text}
          </p>

        </div>

        <span>
        →
      </span>

      </Link>
  );
}

/* ==================================================
   ACTIVITIES
================================================== */

function Activities() {
  const [activities, setActivities] =
      useState([]);

  const [categories, setCategories] =
      useState([]);

  const [locations, setLocations] =
      useState([]);

  const [categoryId, setCategoryId] =
      useState("");

  const [locationId, setLocationId] =
      useState("");

  const [page, setPage] =
      useState(0);

  const [totalPages, setTotalPages] =
      useState(0);

  const [search, setSearch] =
      useState("");

  const [loading, setLoading] =
      useState(true);

  useEffect(() => {
    categoriesApi
        .getAll()
        .then((response) =>
            setCategories(
                response.data
            )
        )
        .catch(() => {});

    locationsApi
        .getAll()
        .then((response) =>
            setLocations(
                response.data
            )
        )
        .catch(() => {});
  }, []);

  useEffect(() => {
    loadActivities();
  }, [
    page,
    categoryId,
    locationId,
  ]);

  const loadActivities =
      async () => {
        setLoading(true);

        try {
          const response =
              await activitiesApi.getAll(
                  {
                    page,
                    size: 9,
                    categoryId:
                        categoryId ||
                        undefined,
                    locationId:
                        locationId ||
                        undefined,
                    sort:
                        "id,desc",
                  }
              );

          setActivities(
              response.data.content ||
              []
          );

          setTotalPages(
              response.data.totalPages ||
              0
          );

        } catch (error) {
          console.error(error);

          setActivities([]);
        } finally {
          setLoading(false);
        }
      };

  const filtered =
      useMemo(() => {
        const query =
            search
                .trim()
                .toLowerCase();

        if (!query) {
          return activities;
        }

        return activities.filter(
            (activity) =>
                activity.title
                    ?.toLowerCase()
                    .includes(query) ||
                activity.description
                    ?.toLowerCase()
                    .includes(query)
        );
      }, [
        activities,
        search,
      ]);

  return (
      <Layout>

        <div className="page">

          <SectionHeader
              title="Explore Activities"
              text="Find something you want to experience."
          />

          <div className="filters">

            <input
                placeholder="Search activities..."
                value={search}
                onChange={(event) =>
                    setSearch(
                        event.target.value
                    )
                }
            />

            <select
                value={categoryId}
                onChange={(event) => {
                  setCategoryId(
                      event.target.value
                  );

                  setPage(0);
                }}
            >
              <option value="">
                All Categories
              </option>

              {categories.map(
                  (category) => (
                      <option
                          key={
                            category.category_id
                          }
                          value={
                            category.category_id
                          }
                      >
                        {category.category_name}
                      </option>
                  )
              )}
            </select>

            <select
                value={locationId}
                onChange={(event) => {
                  setLocationId(
                      event.target.value
                  );

                  setPage(0);
                }}
            >
              <option value="">
                All Locations
              </option>

              {locations.map(
                  (location) => (
                      <option
                          key={location.id}
                          value={location.id}
                      >
                        {location.name}
                      </option>
                  )
              )}
            </select>

          </div>

          {loading ? (
              <Loading />
          ) : filtered.length ===
          0 ? (
              <EmptyState
                  title="No activities found"
                  text="Try changing your filters."
              />
          ) : (
              <div className="activity-grid">

                {filtered.map(
                    (activity) => (
                        <ActivityCard
                            key={
                              activity.activity_id
                            }
                            activity={
                              activity
                            }
                        />
                    )
                )}

              </div>
          )}

          {totalPages > 1 && (
              <div className="pagination">

                <button
                    disabled={page === 0}
                    onClick={() =>
                        setPage(
                            page - 1
                        )
                    }
                >
                  ← Previous
                </button>

                <span>
              Page {page + 1} of{" "}
                  {totalPages}
            </span>

                <button
                    disabled={
                        page >=
                        totalPages - 1
                    }
                    onClick={() =>
                        setPage(
                            page + 1
                        )
                    }
                >
                  Next →
                </button>

              </div>
          )}

        </div>

      </Layout>
  );
}

/* ==================================================
   ACTIVITY CARD
================================================== */

function ActivityCard({
                        activity,
                      }) {
  return (
      <div className="activity-card">

        <div className="activity-image">
          ✦
        </div>

        <div className="activity-content">

        <span className="status-pill">
          {activity.status}
        </span>

          <h3>
            {activity.title}
          </h3>

          <p>
            {activity.description}
          </p>

          <div className="activity-footer">

            <strong>
              {formatMoney(
                  activity.pricePerPerson
              )}
            </strong>

            <Link
                className="small-button"
                to={`/activities/${activity.activity_id}`}
            >
              View
            </Link>

          </div>

        </div>

      </div>
  );
}

/* ==================================================
   ACTIVITY DETAILS
================================================== */

function ActivityDetails() {
  const { id } =
      useParams();

  const [activity, setActivity] =
      useState(null);

  const [sessions, setSessions] =
      useState([]);

  const [loading, setLoading] =
      useState(true);

  const [error, setError] =
      useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const [
          activityResponse,
          sessionsResponse,
        ] = await Promise.all([
          activitiesApi.getById(id),
          sessionsApi.getByActivity(id),
        ]);

        setActivity(
            activityResponse.data
        );

        setSessions(
            sessionsResponse.data || []
        );

      } catch (error) {
        setError(
            getError(error)
        );
      } finally {
        setLoading(false);
      }
    };

    load();
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
            <div className="error-box">
              {error ||
                  "Activity not found."}
            </div>
          </div>
        </Layout>
    );
  }

  return (
      <Layout>

        <div className="page">

          <Link
              className="back-link"
              to="/activities"
          >
            ← Back to activities
          </Link>

          <div className="activity-detail">

            <div className="detail-image">
              ✦
            </div>

            <div>

            <span className="status-pill">
              {activity.status}
            </span>

              <h1>
                {activity.title}
              </h1>

              <p className="detail-description">
                {activity.description}
              </p>

              <div className="detail-price">
                {formatMoney(
                    activity.pricePerPerson
                )}

                <small>
                  per person
                </small>
              </div>

              <p>
                Duration:{" "}
                <strong>
                  {
                    activity.durationMinutes
                  }{" "}
                  minutes
                </strong>
              </p>

            </div>

          </div>

          <SectionHeader
              title="Available Sessions"
              text="Choose a session to continue with your booking."
          />

          <div className="session-list">

            {sessions.length ===
            0 ? (
                <EmptyState
                    title="No sessions"
                    text="There are currently no sessions for this activity."
                />
            ) : (
                sessions.map(
                    (session) => (
                        <SessionCard
                            key={session.id}
                            session={
                              session
                            }
                        />
                    )
                )
            )}

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   SESSION CARD
================================================== */

function SessionCard({
                       session,
                     }) {
  const unavailable =
      session.status !==
      "SCHEDULED" ||
      session.spotsLeft <= 0;

  return (
      <div className="session-card">

        <div>

          <strong>
            {formatDate(
                session.startTime
            )}
          </strong>

          <span>
          Until{" "}
            {formatDate(
                session.endTime
            )}
        </span>

          <span>
          {session.spotsLeft} spots
          remaining
        </span>

        </div>

        <div className="session-right">

        <span
            className={`status-pill ${session.status.toLowerCase()}`}
        >
          {session.status}
        </span>

          {!unavailable ? (
              <Link
                  className="primary-button"
                  to={`/book/${session.id}`}
              >
                Book
              </Link>
          ) : (
              <button
                  className="secondary-button"
                  disabled
              >
                Unavailable
              </button>
          )}

        </div>

      </div>
  );
}

/* ==================================================
   BOOKING
================================================== */

function Booking() {
  const { id } =
      useParams();

  const navigate =
      useNavigate();

  const [session, setSession] =
      useState(null);

  const [activity, setActivity] =
      useState(null);

  const [participants, setParticipants] =
      useState(1);

  const [bookingType, setBookingType] =
      useState("INDIVIDUAL");

  const [loading, setLoading] =
      useState(true);

  const [submitting, setSubmitting] =
      useState(false);

  const [error, setError] =
      useState("");

  useEffect(() => {
    const load = async () => {
      try {
        const sessionResponse =
            await sessionsApi.getById(
                id
            );

        setSession(
            sessionResponse.data
        );

        const activityResponse =
            await activitiesApi.getById(
                sessionResponse.data.activityId
            );

        setActivity(
            activityResponse.data
        );

      } catch (error) {
        setError(
            getError(error)
        );
      } finally {
        setLoading(false);
      }
    };

    load();
  }, [id]);

  const submit = async (
      event
  ) => {
    event.preventDefault();

    setError("");

    if (!session) {
      return;
    }

    if (
        participants < 1
    ) {
      setError(
          "You must select at least one participant."
      );

      return;
    }

    if (
        participants >
        session.spotsLeft
    ) {
      setError(
          `Only ${session.spotsLeft} spots are available.`
      );

      return;
    }

    setSubmitting(true);

    try {
      await bookingsApi.create(
          {
            sessionId:
                Number(id),

            participants:
                Number(
                    participants
                ),

            bookingType,
          }
      );

      navigate(
          "/bookings"
      );

    } catch (error) {
      setError(
          getError(error)
      );
    } finally {
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

  return (
      <Layout>

        <div className="page narrow-page">

          <Link
              className="back-link"
              to={
                activity
                    ? `/activities/${activity.activity_id}`
                    : "/activities"
              }
          >
            ← Back
          </Link>

          <div className="booking-card">

          <span className="eyebrow">
            RESERVATION
          </span>

            <h1>
              Book your session
            </h1>

            {activity && (
                <div className="booking-summary">

                  <strong>
                    {activity.title}
                  </strong>

                  <span>
                {formatDate(
                    session?.startTime
                )}
              </span>

                  <span>
                {formatMoney(
                    activity.pricePerPerson
                )}{" "}
                    per person
              </span>

                </div>
            )}

            <form
                onSubmit={submit}
            >

              <label className="field">
              <span>
                Participants
              </span>

                <input
                    type="number"
                    min="1"
                    max={
                        session?.spotsLeft ||
                        1
                    }
                    value={participants}
                    onChange={(event) =>
                        setParticipants(
                            Number(
                                event.target.value
                            )
                        )
                    }
                />

                <small>
                  {
                    session?.spotsLeft
                  }{" "}
                  spots available
                </small>
              </label>

              <label className="field">
              <span>
                Booking Type
              </span>

                <select
                    value={
                      bookingType
                    }
                    onChange={(event) =>
                        setBookingType(
                            event.target.value
                        )
                    }
                >
                  <option value="INDIVIDUAL">
                    Individual
                  </option>

                  <option value="GROUP">
                    Group
                  </option>
                </select>
              </label>

              {error && (
                  <div className="error-box">
                    {error}
                  </div>
              )}

              <button
                  className="primary-button full"
                  disabled={submitting}
              >
                {submitting
                    ? "Booking..."
                    : "Confirm Booking"}
              </button>

            </form>

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   BOOKINGS
================================================== */

function Bookings() {
  const [bookings, setBookings] =
      useState([]);

  const [status, setStatus] =
      useState("");

  const [loading, setLoading] =
      useState(true);

  const [error, setError] =
      useState("");

  const load = async () => {
    setLoading(true);

    try {
      const response =
          await bookingsApi.getAll(
              {
                page: 0,
                size: 50,
                status:
                    status || undefined,
                sort:
                    "createdAt,desc",
              }
          );

      setBookings(
          response.data.content ||
          []
      );

    } catch (error) {
      setError(
          getError(error)
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [status]);

  const cancel = async (
      bookingId
  ) => {
    const confirmed =
        window.confirm(
            "Are you sure you want to cancel this booking?"
        );

    if (!confirmed) {
      return;
    }

    try {
      await bookingsApi.cancel(
          bookingId
      );

      await load();

    } catch (error) {
      setError(
          getError(error)
      );
    }
  };

  return (
      <Layout>

        <div className="page">

          <SectionHeader
              title="My Bookings"
              text="Track and manage your reservations."
          />

          <div className="tabs">

            {[
              "",
              "PENDING_PAYMENT",
              "CONFIRMED",
              "COMPLETED",
              "CANCELLED",
            ].map(
                (value) => (
                    <button
                        key={value}
                        className={
                          status === value
                              ? "active"
                              : ""
                        }
                        onClick={() =>
                            setStatus(value)
                        }
                    >
                      {value
                          ? value.replace(
                              "_",
                              " "
                          )
                          : "ALL"}
                    </button>
                )
            )}

          </div>

          {error && (
              <div className="error-box">
                {error}
              </div>
          )}

          {loading ? (
              <Loading />
          ) : bookings.length ===
          0 ? (
              <EmptyState
                  title="No bookings"
                  text="Your bookings will appear here."
              />
          ) : (
              <div className="booking-list">

                {bookings.map(
                    (booking) => (
                        <BookingCard
                            key={booking.id}
                            booking={
                              booking
                            }
                            onCancel={
                              cancel
                            }
                        />
                    )
                )}

              </div>
          )}

        </div>

      </Layout>
  );
}

/* ==================================================
   BOOKING CARD
================================================== */

function BookingCard({
                       booking,
                       onCancel,
                     }) {
  const canCancel =
      booking.status ===
      "PENDING_PAYMENT" ||
      booking.status ===
      "CONFIRMED";

  return (
      <div className="booking-card-row">

        <div className="booking-icon">
          ▣
        </div>

        <div className="booking-info">

          <h3>
            Booking #
            {booking.id}
          </h3>

          <p>
            Session #
            {booking.sessionId}
          </p>

          <span>
          {booking.participants}{" "}
            participant
            {booking.participants !==
            1
                ? "s"
                : ""}
        </span>

          <span>
          {booking.bookingType}
        </span>

        </div>

        <div className="booking-price">

          <strong>
            {formatMoney(
                booking.totalPrice
            )}
          </strong>

          <span
              className={`status-pill ${booking.status.toLowerCase()}`}
          >
          {booking.status.replace(
              "_",
              " "
          )}
        </span>

          <small>
            {booking.paymentStatus}
          </small>

          {canCancel && (
              <button
                  className="danger-button"
                  onClick={() =>
                      onCancel(
                          booking.id
                      )
                  }
              >
                Cancel
              </button>
          )}

        </div>

      </div>
  );
}

/* ==================================================
   PROFILE
================================================== */

function Profile() {
  const {
    user,
    refreshUser,
  } = useAuth();

  const [name, setName] =
      useState("");

  const [phone, setPhone] =
      useState("");

  const [currentPassword, setCurrentPassword] =
      useState("");

  const [newPassword, setNewPassword] =
      useState("");

  const [message, setMessage] =
      useState("");

  const [error, setError] =
      useState("");

  useEffect(() => {
    if (user) {
      setName(
          user.fullName || ""
      );

      setPhone(
          user.phone || ""
      );
    }
  }, [user]);

  const updateProfile =
      async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");

        try {
          await usersApi.updateMe(
              {
                fullName: name,
                phone,
              }
          );

          await refreshUser();

          setMessage(
              "Profile updated successfully."
          );

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  const uploadPicture =
      async (event) => {
        const file =
            event.target.files?.[0];

        if (!file) {
          return;
        }

        try {
          await usersApi.uploadProfilePicture(
              file
          );

          await refreshUser();

          setMessage(
              "Profile picture uploaded."
          );

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  const changePassword =
      async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");

        if (
            newPassword.length < 8
        ) {
          setError(
              "New password must contain at least 8 characters."
          );

          return;
        }

        try {
          await authApi.changePassword(
              {
                currentPassword,
                newPassword,
              }
          );

          setCurrentPassword("");
          setNewPassword("");

          setMessage(
              "Password changed successfully."
          );

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  return (
      <Layout>

        <div className="page">

          <SectionHeader
              title="Profile & Settings"
              text="Manage your account."
          />

          <div className="profile-grid">

            <div className="profile-card">

              <div className="avatar">
                {user?.fullName
                        ?.charAt(0)
                        ?.toUpperCase() ||
                    "U"}
              </div>

              <h2>
                {user?.fullName}
              </h2>

              <p>
                {user?.email}
              </p>

              <span className="status-pill">
              {user?.role}
            </span>

              <label className="upload-box">
                Upload Profile Picture

                <input
                    type="file"
                    accept="image/*"
                    onChange={
                      uploadPicture
                    }
                />
              </label>

            </div>

            <div className="profile-forms">

              <form
                  className="form-card"
                  onSubmit={
                    updateProfile
                  }
              >

                <h2>
                  Personal Information
                </h2>

                <Input
                    label="Full name"
                    value={name}
                    onChange={
                      setName
                    }
                />

                <Input
                    label="Phone"
                    value={phone}
                    onChange={
                      setPhone
                    }
                />

                <button className="primary-button">
                  Save Changes
                </button>

              </form>

              <form
                  className="form-card"
                  onSubmit={
                    changePassword
                  }
              >

                <h2>
                  Change Password
                </h2>

                <Input
                    label="Current password"
                    type="password"
                    value={
                      currentPassword
                    }
                    onChange={
                      setCurrentPassword
                    }
                />

                <Input
                    label="New password"
                    type="password"
                    value={
                      newPassword
                    }
                    onChange={
                      setNewPassword
                    }
                />

                <button className="secondary-button">
                  Change Password
                </button>

              </form>

              {error && (
                  <div className="error-box">
                    {error}
                  </div>
              )}

              {message && (
                  <div className="success-box">
                    {message}
                  </div>
              )}

            </div>

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   PROVIDER
================================================== */

function Provider() {
  const [categories, setCategories] =
      useState([]);

  const [locations, setLocations] =
      useState([]);

  const [activities, setActivities] =
      useState([]);

  const [activityForm, setActivityForm] =
      useState({
        title: "",
        description: "",
        pricePerPerson: "",
        durationMinutes: "",
        categoryId: "",
        locationId: "",
      });

  const [sessionForm, setSessionForm] =
      useState({
        activity_id: "",
        startTime: "",
        endTime: "",
        capacity: 10,
      });

  const [message, setMessage] =
      useState("");

  const [error, setError] =
      useState("");

  const loadActivities =
      async () => {
        try {
          const response =
              await activitiesApi.getAll(
                  {
                    page: 0,
                    size: 100,
                  }
              );

          setActivities(
              response.data.content ||
              []
          );
        } catch {
          setActivities([]);
        }
      };

  useEffect(() => {
    categoriesApi
        .getAll()
        .then((response) =>
            setCategories(
                response.data
            )
        );

    locationsApi
        .getAll()
        .then((response) =>
            setLocations(
                response.data
            )
        );

    loadActivities();
  }, []);

  const createActivity =
      async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");

        try {
          const response =
              await activitiesApi.create(
                  {
                    title:
                    activityForm.title,

                    description:
                    activityForm.description,

                    pricePerPerson:
                        Number(
                            activityForm.pricePerPerson
                        ),

                    durationMinutes:
                        Number(
                            activityForm.durationMinutes
                        ),

                    categoryId:
                        Number(
                            activityForm.categoryId
                        ),

                    locationId:
                        Number(
                            activityForm.locationId
                        ),
                  }
              );

          setMessage(
              "Activity created successfully."
          );

          setSessionForm(
              (previous) => ({
                ...previous,
                activity_id:
                response.data
                    .activity_id,
              })
          );

          await loadActivities();

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  const createSession =
      async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");

        try {
          await sessionsApi.create(
              {
                activity_id:
                    Number(
                        sessionForm.activity_id
                    ),

                startTime:
                sessionForm.startTime,

                endTime:
                sessionForm.endTime,

                capacity:
                    Number(
                        sessionForm.capacity
                    ),
              }
          );

          setMessage(
              "Session created successfully."
          );

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  return (
      <Layout>

        <div className="page">

          <SectionHeader
              title="Provider Dashboard"
              text="Create activities and sessions for testing."
          />

          {message && (
              <div className="success-box">
                {message}
              </div>
          )}

          {error && (
              <div className="error-box">
                {error}
              </div>
          )}

          <div className="two-column">

            <form
                className="form-card"
                onSubmit={
                  createActivity
                }
            >

              <h2>
                Create Activity
              </h2>

              <Input
                  label="Title"
                  value={
                    activityForm.title
                  }
                  onChange={(value) =>
                      setActivityForm(
                          {
                            ...activityForm,
                            title: value,
                          }
                      )
                  }
              />

              <label className="field">
              <span>
                Description
              </span>

                <textarea
                    value={
                      activityForm.description
                    }
                    onChange={(event) =>
                        setActivityForm(
                            {
                              ...activityForm,
                              description:
                              event.target
                                  .value,
                            }
                        )
                    }
                />
              </label>

              <Input
                  label="Price per person"
                  type="number"
                  value={
                    activityForm.pricePerPerson
                  }
                  onChange={(value) =>
                      setActivityForm(
                          {
                            ...activityForm,
                            pricePerPerson:
                            value,
                          }
                      )
                  }
              />

              <Input
                  label="Duration in minutes"
                  type="number"
                  value={
                    activityForm.durationMinutes
                  }
                  onChange={(value) =>
                      setActivityForm(
                          {
                            ...activityForm,
                            durationMinutes:
                            value,
                          }
                      )
                  }
              />

              <Select
                  label="Category"
                  value={
                    activityForm.categoryId
                  }
                  onChange={(value) =>
                      setActivityForm(
                          {
                            ...activityForm,
                            categoryId:
                            value,
                          }
                      )
                  }
                  options={
                    categories.map(
                        (category) => [
                          category.category_id,
                          category.category_name,
                        ]
                    )
                  }
              />

              <Select
                  label="Location"
                  value={
                    activityForm.locationId
                  }
                  onChange={(value) =>
                      setActivityForm(
                          {
                            ...activityForm,
                            locationId:
                            value,
                          }
                      )
                  }
                  options={
                    locations.map(
                        (location) => [
                          location.id,
                          location.name,
                        ]
                    )
                  }
              />

              <button className="primary-button">
                Create Activity
              </button>

            </form>

            <form
                className="form-card"
                onSubmit={
                  createSession
                }
            >

              <h2>
                Create Session
              </h2>

              <Select
                  label="Activity"
                  value={
                    sessionForm.activity_id
                  }
                  onChange={(value) =>
                      setSessionForm(
                          {
                            ...sessionForm,
                            activity_id:
                            value,
                          }
                      )
                  }
                  options={
                    activities.map(
                        (activity) => [
                          activity.activity_id,
                          activity.title,
                        ]
                    )
                  }
              />

              <Input
                  label="Start"
                  type="datetime-local"
                  value={
                    sessionForm.startTime
                  }
                  onChange={(value) =>
                      setSessionForm(
                          {
                            ...sessionForm,
                            startTime:
                            value,
                          }
                      )
                  }
              />

              <Input
                  label="End"
                  type="datetime-local"
                  value={
                    sessionForm.endTime
                  }
                  onChange={(value) =>
                      setSessionForm(
                          {
                            ...sessionForm,
                            endTime:
                            value,
                          }
                      )
                  }
              />

              <Input
                  label="Capacity"
                  type="number"
                  value={
                    sessionForm.capacity
                  }
                  onChange={(value) =>
                      setSessionForm(
                          {
                            ...sessionForm,
                            capacity:
                            value,
                          }
                      )
                  }
              />

              <button className="primary-button">
                Create Session
              </button>

            </form>

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   ADMIN
================================================== */

function Admin() {
  const [applications, setApplications] =
      useState([]);

  const [auditLogs, setAuditLogs] =
      useState([]);

  const [userId, setUserId] =
      useState("");

  const [role, setRole] =
      useState("USER");

  const [reviewNote, setReviewNote] =
      useState("");

  const [message, setMessage] =
      useState("");

  const [error, setError] =
      useState("");

  const load = async () => {
    try {
      const [
        applicationsResponse,
        logsResponse,
      ] = await Promise.all([
        applicationsApi.getAll(),

        adminApi.getAuditLogs(
            {
              page: 0,
              size: 20,
              sort:
                  "createdAt,desc",
            }
        ),
      ]);

      setApplications(
          applicationsResponse.data ||
          []
      );

      setAuditLogs(
          logsResponse.data.content ||
          []
      );

    } catch (error) {
      setError(
          getError(error)
      );
    }
  };

  useEffect(() => {
    load();
  }, []);

  const updateRole =
      async () => {
        setMessage("");
        setError("");

        if (!userId) {
          setError(
              "Enter a user ID."
          );

          return;
        }

        try {
          await adminApi.updateRole(
              Number(userId),
              role
          );

          setMessage(
              "User role updated."
          );

          await load();

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  const deactivate =
      async () => {
        setMessage("");
        setError("");

        if (!userId) {
          setError(
              "Enter a user ID."
          );

          return;
        }

        try {
          await adminApi.deactivateUser(
              Number(userId)
          );

          setMessage(
              "User deactivated."
          );

          await load();

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  const reviewApplication =
      async (
          applicationId,
          action
      ) => {
        setMessage("");
        setError("");

        try {
          if (
              action === "approve"
          ) {
            await applicationsApi.approve(
                applicationId,
                reviewNote
            );
          } else {
            await applicationsApi.reject(
                applicationId,
                reviewNote
            );
          }

          setMessage(
              `Application ${action}d.`
          );

          setReviewNote("");

          await load();

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  return (
      <Layout>

        <div className="page">

          <SectionHeader
              title="Admin Panel"
              text="Manage users, provider applications and audit logs."
          />

          {message && (
              <div className="success-box">
                {message}
              </div>
          )}

          {error && (
              <div className="error-box">
                {error}
              </div>
          )}

          <div className="stats-grid">

            <DashboardStat
                value={
                  applications.length
                }
                label="Provider Applications"
            />

            <DashboardStat
                value={
                  auditLogs.length
                }
                label="Recent Audit Logs"
            />

            <DashboardStat
                value="ADMIN"
                label="Access Level"
            />

            <DashboardStat
                value="LIVE"
                label="Backend"
            />

          </div>

          <div className="two-column">

            <div className="form-card">

              <h2>
                User Management
              </h2>

              <p className="muted">
                Your current backend exposes
                role updates and user
                deactivation by user ID.
              </p>

              <Input
                  label="User ID"
                  type="number"
                  value={userId}
                  onChange={
                    setUserId
                  }
              />

              <Select
                  label="Role"
                  value={role}
                  onChange={
                    setRole
                  }
                  options={[
                    [
                      "USER",
                      "USER",
                    ],
                    [
                      "PROVIDER",
                      "PROVIDER",
                    ],
                    [
                      "ADMIN",
                      "ADMIN",
                    ],
                  ]}
              />

              <div className="button-row">

                <button
                    className="primary-button"
                    onClick={
                      updateRole
                    }
                >
                  Update Role
                </button>

                <button
                    className="danger-button"
                    onClick={
                      deactivate
                    }
                >
                  Deactivate
                </button>

              </div>

            </div>

            <div className="form-card">

              <h2>
                Provider Applications
              </h2>

              <Input
                  label="Review note"
                  value={reviewNote}
                  onChange={
                    setReviewNote
                  }
              />

              <div className="application-list">

                {applications.length ===
                0 ? (
                    <EmptyState
                        title="No applications"
                        text="Provider applications will appear here."
                    />
                ) : (
                    applications.map(
                        (application) => (
                            <div
                                className="application"
                                key={
                                  application.application_id
                                }
                            >

                              <div>

                                <strong>
                                  {
                                    application.businessName
                                  }
                                </strong>

                                <span>
                          {
                            application.contactName
                          }
                        </span>

                                <span>
                          {
                            application.city
                          }
                        </span>

                                <span>
                          Status:{" "}
                                  {
                                    application.status
                                  }
                        </span>

                              </div>

                              {application.status ===
                                  "PENDING" && (
                                      <div className="button-row">

                                        <button
                                            className="small-button"
                                            onClick={() =>
                                                reviewApplication(
                                                    application.application_id,
                                                    "approve"
                                                )
                                            }
                                        >
                                          Approve
                                        </button>

                                        <button
                                            className="danger-button small"
                                            onClick={() =>
                                                reviewApplication(
                                                    application.application_id,
                                                    "reject"
                                                )
                                            }
                                        >
                                          Reject
                                        </button>

                                      </div>
                                  )}

                            </div>
                        )
                    )
                )}

              </div>

            </div>

          </div>

          <div className="form-card">

            <h2>
              Audit Logs
            </h2>

            {auditLogs.length ===
            0 ? (
                <EmptyState
                    title="No audit logs"
                    text="Audit events will appear here."
                />
            ) : (
                <div className="table-container">

                  <table>

                    <thead>
                    <tr>
                      <th>
                        ID
                      </th>

                      <th>
                        Action
                      </th>

                      <th>
                        Entity
                      </th>

                      <th>
                        Created
                      </th>
                    </tr>
                    </thead>

                    <tbody>

                    {auditLogs.map(
                        (log, index) => (
                            <tr
                                key={
                                    log.id ||
                                    index
                                }
                            >

                              <td>
                                {
                                    log.id ||
                                    "—"
                                }
                              </td>

                              <td>
                                {
                                    log.action ||
                                    log.eventType ||
                                    "—"
                                }
                              </td>

                              <td>
                                {
                                    log.entityType ||
                                    log.entityId ||
                                    "—"
                                }
                              </td>

                              <td>
                                {formatDate(
                                    log.createdAt
                                )}
                              </td>

                            </tr>
                        )
                    )}

                    </tbody>

                  </table>

                </div>
            )}

          </div>

        </div>

      </Layout>
  );
}

/* ==================================================
   PROVIDER APPLICATION
================================================== */

function ApplyProvider() {
  const [form, setForm] =
      useState({
        businessName: "",
        contactName: "",
        phone: "",
        city: "",
        description: "",
        proposedActivities:
            "",
      });

  const [message, setMessage] =
      useState("");

  const [error, setError] =
      useState("");

  const update = (
      field,
      value
  ) => {
    setForm({
      ...form,
      [field]: value,
    });
  };

  const submit =
      async (event) => {
        event.preventDefault();

        setMessage("");
        setError("");

        try {
          await applicationsApi.submit(
              form
          );

          setMessage(
              "Provider application submitted successfully."
          );

        } catch (error) {
          setError(
              getError(error)
          );
        }
      };

  return (
      <Layout>

        <div className="auth-page">

          <form
              className="auth-card"
              onSubmit={submit}
          >

          <span className="eyebrow">
            BECOME A PROVIDER
          </span>

            <h1>
              Provider Application
            </h1>

            <Input
                label="Business name"
                value={
                  form.businessName
                }
                onChange={(value) =>
                    update(
                        "businessName",
                        value
                    )
                }
            />

            <Input
                label="Contact name"
                value={
                  form.contactName
                }
                onChange={(value) =>
                    update(
                        "contactName",
                        value
                    )
                }
            />

            <Input
                label="Phone"
                value={
                  form.phone
                }
                onChange={(value) =>
                    update(
                        "phone",
                        value
                    )
                }
            />

            <Input
                label="City"
                value={
                  form.city
                }
                onChange={(value) =>
                    update(
                        "city",
                        value
                    )
                }
            />

            <Input
                label="Proposed activities"
                value={
                  form.proposedActivities
                }
                onChange={(value) =>
                    update(
                        "proposedActivities",
                        value
                    )
                }
            />

            <label className="field">

            <span>
              Description
            </span>

              <textarea
                  value={
                    form.description
                  }
                  onChange={(event) =>
                      update(
                          "description",
                          event.target.value
                      )
                  }
              />

            </label>

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            <button className="primary-button full">
              Submit Application
            </button>

          </form>

        </div>

      </Layout>
  );
}

/* ==================================================
   INPUT
================================================== */

function Input({
                 label,
                 type = "text",
                 value,
                 onChange,
               }) {
  return (
      <label className="field">

      <span>
        {label}
      </span>

        <input
            type={type}
            value={value ?? ""}
            onChange={(event) =>
                onChange(
                    event.target.value
                )
            }
        />

      </label>
  );
}

/* ==================================================
   SELECT
================================================== */

function Select({
                  label,
                  value,
                  onChange,
                  options,
                }) {
  return (
      <label className="field">

      <span>
        {label}
      </span>

        <select
            value={value ?? ""}
            onChange={(event) =>
                onChange(
                    event.target.value
                )
            }
        >

          <option value="">
            Select...
          </option>

          {options.map(
              ([optionValue, label]) => (
                  <option
                      key={optionValue}
                      value={optionValue}
                  >
                    {label}
                  </option>
              )
          )}

        </select>

      </label>
  );
}

/* ==================================================
   EMPTY
================================================== */

function EmptyState({
                      title,
                      text,
                    }) {
  return (
      <div className="empty-state">

        <div>
          ◌
        </div>

        <h3>
          {title}
        </h3>

        <p>
          {text}
        </p>

      </div>
  );
}

/* ==================================================
   404
================================================== */

function NotFound() {
  return (
      <Layout>

        <div className="loading-page">

          <h1>
            404
          </h1>

          <p>
            Page not found.
          </p>

          <Link
              className="primary-button"
              to="/"
          >
            Go Home
          </Link>

        </div>

      </Layout>
  );
}

/* ==================================================
   APP ROUTES
================================================== */

export default function App() {
  return (
      <Routes>

        {/* PUBLIC */}

        <Route
            path="/"
            element={<Landing />}
        />

        <Route
            path="/login"
            element={<Login />}
        />

        <Route
            path="/register"
            element={<Register />}
        />

        {/* This page requires login because
          your backend application endpoint
          is authenticated. */}

        <Route
            path="/apply-provider"
            element={
              <Protected>
                <ApplyProvider />
              </Protected>
            }
        />

        {/* USER */}

        <Route
            path="/dashboard"
            element={
              <Protected>
                <Dashboard />
              </Protected>
            }
        />

        <Route
            path="/activities"
            element={
              <Protected>
                <Activities />
              </Protected>
            }
        />

        <Route
            path="/activities/:id"
            element={
              <Protected>
                <ActivityDetails />
              </Protected>
            }
        />

        <Route
            path="/book/:id"
            element={
              <Protected>
                <Booking />
              </Protected>
            }
        />

        <Route
            path="/bookings"
            element={
              <Protected>
                <Bookings />
              </Protected>
            }
        />

        <Route
            path="/profile"
            element={
              <Protected>
                <Profile />
              </Protected>
            }
        />

        {/* PROVIDER */}

        <Route
            path="/provider"
            element={
              <Protected
                  roles={[
                    "PROVIDER",
                    "ADMIN",
                  ]}
              >
                <Provider />
              </Protected>
            }
        />

        {/* ADMIN */}

        <Route
            path="/admin"
            element={
              <Protected
                  roles={["ADMIN"]}
              >
                <Admin />
              </Protected>
            }
        />

        {/* FALLBACK */}

        <Route
            path="*"
            element={<NotFound />}
        />

      </Routes>
  );
}