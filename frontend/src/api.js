import axios from "axios";

export const API_URL =
    import.meta.env.VITE_API_URL || "http://localhost:8080";

export const api = axios.create({
    baseURL: API_URL,
    headers: {
        "Content-Type": "application/json",
    },
});

/* --------------------------------------------------
   JWT INTERCEPTOR
-------------------------------------------------- */

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem("bookngo_token");

        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }

        return config;
    },
    (error) => Promise.reject(error)
);

/* --------------------------------------------------
   GLOBAL ERROR HANDLER
-------------------------------------------------- */

api.interceptors.response.use(
    (response) => response,

    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem("bookngo_token");
            localStorage.removeItem("bookngo_role");

            if (!window.location.pathname.startsWith("/login")) {
                window.location.href = "/login";
            }
        }

        const backendMessage =
            error.response?.data?.message ||
            error.response?.data?.error ||
            error.response?.data;

        error.userMessage =
            typeof backendMessage === "string"
                ? backendMessage
                : error.message || "Something went wrong.";

        return Promise.reject(error);
    }
);

/* --------------------------------------------------
   AUTH
-------------------------------------------------- */

export const authApi = {
    register: (data) =>
        api.post("/api/auth/register", data),

    login: (data) =>
        api.post("/api/auth/login", data),

    verify: (token) =>
        api.get("/api/auth/verify", {
            params: { token },
        }),

    forgotPassword: (data) =>
        api.post("/api/auth/forgotPassword", data),

    resetPassword: (data) =>
        api.post("/api/auth/resetPassword", data),

    changePassword: (data) =>
        api.put("/api/auth/changePassword", data),
};

/* --------------------------------------------------
   USER PROFILE
-------------------------------------------------- */

export const usersApi = {
    getMe: () =>
        api.get("/api/users/me"),

    updateMe: (data) =>
        api.put("/api/users/me", data),

    uploadProfilePicture: (file) => {
        const formData = new FormData();

        formData.append("file", file);

        return api.post(
            "/api/users/me/profile-picture",
            formData,
            {
                headers: {
                    "Content-Type": "multipart/form-data",
                },
            }
        );
    },
};

/* --------------------------------------------------
   ACTIVITIES
-------------------------------------------------- */

export const activitiesApi = {
    getAll: (params = {}) =>
        api.get("/api/activities", {
            params,
        }),

    getById: (id) =>
        api.get(`/api/activities/${id}`),

    create: (data) =>
        api.post("/api/activities", data),

    update: (id, data) =>
        api.put(`/api/activities/${id}`, data),

    delete: (id) =>
        api.delete(`/api/activities/${id}`),
};

/* --------------------------------------------------
   CATEGORIES
-------------------------------------------------- */

export const categoriesApi = {
    getAll: () =>
        api.get("/api/categories"),

    getById: (id) =>
        api.get(`/api/categories/${id}`),
};

/* --------------------------------------------------
   LOCATIONS
-------------------------------------------------- */

export const locationsApi = {
    getAll: () =>
        api.get("/api/locations"),

    getById: (id) =>
        api.get(`/api/locations/${id}`),
};

/* --------------------------------------------------
   SESSIONS
-------------------------------------------------- */

export const sessionsApi = {
    getByActivity: (activityId) =>
        api.get(`/api/sessions/activity/${activityId}`),

    getById: (id) =>
        api.get(`/api/sessions/${id}`),

    create: (data) =>
        api.post("/api/sessions", data),

    update: (id, data) =>
        api.put(`/api/sessions/${id}`, data),

    updateStatus: (id) =>
        api.patch(`/api/sessions/${id}/status`),

    cancel: (id) =>
        api.delete(`/api/sessions/${id}`),
};

/* --------------------------------------------------
   BOOKINGS
-------------------------------------------------- */

export const bookingsApi = {
    getAll: (params = {}) =>
        api.get("/api/bookings", {
            params,
        }),

    getById: (id) =>
        api.get(`/api/bookings/${id}`),

    create: (data) =>
        api.post("/api/bookings", data),

    cancel: (id) =>
        api.delete(`/api/bookings/${id}`),
};

/* --------------------------------------------------
   PROVIDER APPLICATIONS
-------------------------------------------------- */

export const applicationsApi = {
    submit: (data) =>
        api.post("/api/applications", data),

    getMine: () =>
        api.get("/api/applications/mine"),

    getAll: () =>
        api.get("/api/applications/all"),

    getById: (id) =>
        api.get(`/api/applications/${id}`),

    approve: (id, reviewNote) =>
        api.patch(
            `/api/applications/${id}/status`,
            {
                reviewNote,
            },
            {
                params: {
                    action: "approve",
                },
            }
        ),

    reject: (id, reviewNote) =>
        api.patch(
            `/api/applications/${id}/status`,
            {
                reviewNote,
            },
            {
                params: {
                    action: "reject",
                },
            }
        ),
};

/* --------------------------------------------------
   PROVIDER PROFILE
-------------------------------------------------- */

export const providerApi = {
    getMe: () =>
        api.get("/api/provider-profile/me"),

    updateMe: (data) =>
        api.put("/api/provider-profile/me", data),
};

/* --------------------------------------------------
   ADMIN
-------------------------------------------------- */

export const adminApi = {
    updateRole: (userId, role) =>
        api.put(`/api/admin/${userId}/role`, {
            role,
        }),

    deactivateUser: (userId) =>
        api.put(`/api/admin/users/${userId}/deactivate`),

    getAuditLogs: (params = {}) =>
        api.get("/api/admin/audit-logs", {
            params,
        }),
};

/* --------------------------------------------------
   SSE NOTIFICATIONS
-------------------------------------------------- */

export const notificationUrl =
    () => `${API_URL}/api/notifications/subscribe`;