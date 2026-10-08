import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";

import { ROLE_KEY, TOKEN_KEY, authApi, notificationUrl, notificationsApi, usersApi } from "./api";
import { getTokenExpiry, isTokenExpired } from "./utils/format";
import { readEventStream } from "./utils/sse";

const AuthContext = createContext(null);

const POLL_INTERVAL_MS = 15000;
const SSE_RETRY_MS = 5000;
const TOAST_DURATION_MS = 6000;
const MAX_NOTIFICATIONS = 30;

/** Reads the stored token, discarding it if it has already expired. */
function loadStoredToken() {
  const stored = localStorage.getItem(TOKEN_KEY);
  if (stored && isTokenExpired(stored)) {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    return null;
  }
  return stored;
}

/** Converts an SSE event payload into the same shape the REST API returns. */
function toNotification(eventName, rawData) {
  try {
    const parsed = JSON.parse(rawData);
    if (parsed && typeof parsed === "object") {
      return {
        id: parsed.id ?? `${Date.now()}-${Math.random()}`,
        type: parsed.type || eventName,
        message: parsed.message || rawData,
        read: Boolean(parsed.read),
        createdAt: parsed.createdAt || new Date().toISOString(),
      };
    }
  } catch {
    // Plain text payload, handled below.
  }
  return {
    id: `${Date.now()}-${Math.random()}`,
    type: eventName,
    message: rawData,
    read: false,
    createdAt: new Date().toISOString(),
  };
}

export function AuthProvider({ children }) {
  const [token, setToken] = useState(loadStoredToken);
  const [role, setRole] = useState(() => (loadStoredToken() ? localStorage.getItem(ROLE_KEY) : null));
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(token));
  const [notifications, setNotifications] = useState([]);
  const [toasts, setToasts] = useState([]);

  // Ids we have already seen, so a notification only produces one toast.
  const knownIds = useRef(new Set());
  const initialLoadDone = useRef(false);

  const logout = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(ROLE_KEY);
    knownIds.current = new Set();
    initialLoadDone.current = false;
    setToken(null);
    setRole(null);
    setUser(null);
    setNotifications([]);
    setToasts([]);
  }, []);

  /* ---------- CURRENT USER ---------- */

  const refreshUser = useCallback(async () => {
    const response = await usersApi.getMe();
    setUser(response.data);
    // The role can change while logged in (e.g. provider application approved).
    setRole(response.data.role);
    localStorage.setItem(ROLE_KEY, response.data.role);
    return response.data;
  }, []);

  /* ---------- TOASTS ---------- */

  const dismissToast = useCallback((id) => {
    setToasts((items) => items.filter((item) => item.id !== id));
  }, []);

  const pushToast = useCallback(
    (notification) => {
      setToasts((items) => [notification, ...items].slice(0, 3));
      setTimeout(() => dismissToast(notification.id), TOAST_DURATION_MS);
    },
    [dismissToast]
  );

  /* ---------- NOTIFICATIONS ---------- */

  const refreshUserRef = useRef(refreshUser);
  refreshUserRef.current = refreshUser;

  /** Registers a notification that arrived after the initial load. */
  const handleIncoming = useCallback(
    (notification) => {
      const key = String(notification.id);
      if (knownIds.current.has(key)) {
        return;
      }
      knownIds.current.add(key);

      if (!notification.read) {
        pushToast(notification);
      }
      if (notification.type?.startsWith("provider-application-") && notification.type !== "provider-application-pending") {
        refreshUserRef.current().catch(() => {});
      }
    },
    [pushToast]
  );

  const refreshNotifications = useCallback(async () => {
    try {
      const response = await notificationsApi.getMine();
      const items = response.data || [];

      if (initialLoadDone.current) {
        items.forEach(handleIncoming);
      } else {
        items.forEach((item) => knownIds.current.add(String(item.id)));
        initialLoadDone.current = true;
      }

      setNotifications(items);
      return items;
    } catch (error) {
      console.error("Could not load notifications.", error);
      return [];
    }
  }, [handleIncoming]);

  /* ---------- SESSION BOOTSTRAP ---------- */

  useEffect(() => {
    if (!token) {
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);

    refreshUser()
      .then(() => refreshNotifications())
      .catch(() => {
        // The token is no longer accepted (expired, user deactivated, ...).
        if (!cancelled) {
          logout();
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
  }, [token, refreshUser, refreshNotifications, logout]);

  /* ---------- AUTO LOGOUT WHEN THE JWT EXPIRES ---------- */

  useEffect(() => {
    if (!token) {
      return;
    }
    const expiry = getTokenExpiry(token);
    if (!expiry) {
      return;
    }
    const timer = setTimeout(logout, Math.max(expiry - Date.now(), 0));
    return () => clearTimeout(timer);
  }, [token, logout]);

  /* ---------- POLLING FALLBACK ---------- */

  useEffect(() => {
    if (!token) {
      return;
    }
    const interval = setInterval(refreshNotifications, POLL_INTERVAL_MS);
    return () => clearInterval(interval);
  }, [token, refreshNotifications]);

  /* ---------- SERVER-SENT EVENTS (real-time push) ---------- */

  useEffect(() => {
    if (!token) {
      return;
    }

    const controller = new AbortController();
    let retryTimer;

    const connect = async () => {
      try {
        const response = await fetch(notificationUrl(), {
          headers: { Authorization: `Bearer ${token}`, Accept: "text/event-stream" },
          signal: controller.signal,
        });

        if (response.status === 401 || response.status === 403) {
          return; // Not allowed to subscribe; retrying will not help.
        }
        if (!response.ok || !response.body) {
          throw new Error(`SSE connection failed with status ${response.status}`);
        }

        await readEventStream(response.body, (eventName, rawData) => {
          const incoming = toNotification(eventName, rawData);
          setNotifications((previous) =>
            [incoming, ...previous.filter((item) => String(item.id) !== String(incoming.id))].slice(
              0,
              MAX_NOTIFICATIONS
            )
          );
          handleIncoming(incoming);
        });
      } catch (error) {
        if (error.name === "AbortError") {
          return;
        }
      }
      // The stream ended or failed: reconnect shortly.
      retryTimer = setTimeout(connect, SSE_RETRY_MS);
    };

    connect();

    return () => {
      controller.abort();
      clearTimeout(retryTimer);
    };
  }, [token, handleIncoming]);

  /* ---------- LOGIN ---------- */

  const login = async (credentials) => {
    const response = await authApi.login(credentials);
    const { token: newToken, role: newRole } = response.data;

    localStorage.setItem(TOKEN_KEY, newToken);
    localStorage.setItem(ROLE_KEY, newRole);
    setToken(newToken);
    setRole(newRole);

    return response.data;
  };

  return (
    <AuthContext.Provider
      value={{
        token,
        role,
        user,
        loading,
        login,
        logout,
        refreshUser,
        notifications,
        setNotifications,
        refreshNotifications,
        toasts,
        dismissToast,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
