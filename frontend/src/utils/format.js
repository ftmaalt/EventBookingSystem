import { API_URL } from "../api";

/** Formats a price in Bahraini dinar. */
export function formatMoney(value) {
  if (value === null || value === undefined) {
    return "—";
  }
  return `${Number(value).toFixed(2)} BHD`;
}

/** Formats an ISO date-time string for display in the user's locale. */
export function formatDate(value) {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString([], { dateStyle: "medium", timeStyle: "short" });
}

/** Converts an ISO date-time from the API into a `datetime-local` input value. */
export function toInputDateTime(value) {
  return value ? value.slice(0, 16) : "";
}

/** Extracts a readable message from an Axios error. */
export function getError(error) {
  return error?.userMessage || error?.message || "Something went wrong.";
}

/** Builds an absolute URL for a file served by the backend (e.g. profile pictures). */
export function fileUrl(path) {
  return path ? `${API_URL}${path}` : "";
}

/** Returns the expiry time (ms since epoch) of a JWT, or null if it cannot be read. */
export function getTokenExpiry(token) {
  try {
    const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const { exp } = JSON.parse(atob(payload));
    return exp ? exp * 1000 : null;
  } catch {
    return null;
  }
}

export function isTokenExpired(token) {
  const expiry = getTokenExpiry(token);
  return expiry !== null && expiry <= Date.now();
}

/** Human friendly label for enum values such as PENDING_PAYMENT. */
export function labelize(value) {
  return value ? value.replaceAll("_", " ") : "";
}

const NOTIFICATION_TITLES = {
  "provider-application-pending": "Provider application",
  "provider-application-approved": "Application approved",
  "provider-application-rejected": "Application update",
  "booking-confirmed": "Booking confirmed",
};

/** Heading shown above a notification message. */
export function notificationTitle(type) {
  return NOTIFICATION_TITLES[type] || "BookNGo update";
}
