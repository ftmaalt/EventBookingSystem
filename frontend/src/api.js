import axios from "axios";

export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const TOKEN_KEY = "bookngo_token";
export const ROLE_KEY = "bookngo_role";

export const api = axios.create({
  baseURL: API_URL,
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * Turns whatever the backend returned into one readable sentence.
 * Handles Spring's default error body (message / errors[]) as well as a
 * custom { message } body, and falls back to a status based message.
 */
function buildUserMessage(error) {
  if (!error.response) {
    return "Cannot reach the server. Please check that the backend is running.";
  }

  const { status, data } = error.response;

  if (data && typeof data === "object") {
    if (Array.isArray(data.errors) && data.errors.length > 0) {
      const details = data.errors
        .map((item) => item.defaultMessage || item.message)
        .filter(Boolean);
      if (details.length > 0) {
        return details.join(" ");
      }
    }
    if (data.message && !data.message.startsWith("Validation failed")) {
      return data.message;
    }
  }
  if (typeof data === "string" && data.trim() && !data.startsWith("<")) {
    return data;
  }

  switch (status) {
    case 400:
      return "The request was not valid. Please check your input.";
    case 401:
      return "You are not authorized to do that.";
    case 403:
      return "You do not have permission to do that.";
    case 404:
      return "The requested item was not found.";
    case 409:
      return "This conflicts with existing data.";
    default:
      return status >= 500
        ? "The server had a problem. Please try again."
        : "Something went wrong.";
  }
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    error.userMessage = buildUserMessage(error);
    return Promise.reject(error);
  }
);

/* ---------- AUTH ---------- */

export const authApi = {
  register: (data) => api.post("/api/auth/register", data),
  login: (data) => api.post("/api/auth/login", data),
  verify: (token) => api.get("/api/auth/verify", { params: { token } }),
  forgotPassword: (data) => api.post("/api/auth/forgotPassword", data),
  resetPassword: (data) => api.post("/api/auth/resetPassword", data),
  changePassword: (data) => api.put("/api/auth/changePassword", data),
};

/* ---------- USERS ---------- */

export const usersApi = {
  getMe: () => api.get("/api/users/me"),
  updateMe: (data) => api.put("/api/users/me", data),
  uploadProfilePicture: (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return api.post("/api/users/me/profile-picture", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
  },
};

/* ---------- ACTIVITIES ---------- */

export const activitiesApi = {
  getAll: (params = {}) => api.get("/api/activities", { params }),
  getById: (id) => api.get(`/api/activities/${id}`),
  create: (data) => api.post("/api/activities", data),
  update: (id, data) => api.put(`/api/activities/${id}`, data),
  remove: (id) => api.delete(`/api/activities/${id}`),
};

/* ---------- CATEGORIES ---------- */

export const categoriesApi = {
  getAll: () => api.get("/api/categories"),
  create: (data) => api.post("/api/categories", data),
  update: (id, data) => api.put(`/api/categories/${id}`, data),
  remove: (id) => api.delete(`/api/categories/${id}`),
};

/* ---------- LOCATIONS ---------- */

export const locationsApi = {
  getAll: () => api.get("/api/locations"),
  getById: (id) => api.get(`/api/locations/${id}`),
  create: (data) => api.post("/api/locations", data),
  update: (id, data) => api.put(`/api/locations/${id}`, data),
  remove: (id) => api.delete(`/api/locations/${id}`),
};

/* ---------- SESSIONS ---------- */

export const sessionsApi = {
  getByActivity: (activityId) => api.get(`/api/sessions/activity/${activityId}`),
  getById: (id) => api.get(`/api/sessions/${id}`),
  create: (data) => api.post("/api/sessions", data),
  update: (id, data) => api.put(`/api/sessions/${id}`, data),
  refreshStatus: (id) => api.patch(`/api/sessions/${id}/status`),
  remove: (id) => api.delete(`/api/sessions/${id}`),
};

/* ---------- BOOKINGS ---------- */

export const bookingsApi = {
  getAll: (params = {}) => api.get("/api/bookings", { params }),
  getById: (id) => api.get(`/api/bookings/${id}`),
  create: (data) => api.post("/api/bookings", data),
  cancel: (id) => api.delete(`/api/bookings/${id}`),
};

/* ---------- PROVIDER APPLICATIONS ---------- */

export const applicationsApi = {
  submit: (data) => api.post("/api/applications", data),
  getMine: () => api.get("/api/applications/mine"),
  getAll: () => api.get("/api/applications/all"),
  review: (id, action, reviewNote) =>
    api.patch(`/api/applications/${id}/status`, { reviewNote }, { params: { action } }),
};

/* ---------- PROVIDER ---------- */

export const providerApi = {
  getMe: () => api.get("/api/provider-profile/me"),
  updateMe: (data) => api.put("/api/provider-profile/me", data),
};

/* ---------- VIOLATIONS & PENALTIES ---------- */

export const violationsApi = {
  report: (data) => api.post("/api/violations", data),
};

export const penaltiesApi = {
  pay: (id) => api.put(`/api/penalties/${id}/pay`),
};

/* ---------- ADMIN ---------- */

export const adminApi = {
  updateRole: (userId, role) => api.put(`/api/admin/${userId}/role`, { role }),
  deactivateUser: (userId) => api.put(`/api/admin/users/${userId}/deactivate`),
  getAuditLogs: (params = {}) => api.get("/api/admin/audit-logs", { params }),
};

/* ---------- NOTIFICATIONS ---------- */

export const notificationsApi = {
  getMine: () => api.get("/api/notifications"),
  markAllRead: () => api.patch("/api/notifications/read-all"),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
  remove: (id) => api.delete(`/api/notifications/${id}`),
  clearAll: () => api.delete("/api/notifications"),
};

export const notificationUrl = () => `${API_URL}/api/notifications/subscribe`;
