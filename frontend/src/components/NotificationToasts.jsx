import { useAuth } from "../auth";
import { notificationTitle } from "../utils/format";

/** Short-lived pop-ups for notifications pushed by the server in real time. */
export default function NotificationToasts() {
  const { toasts, dismissToast } = useAuth();

  if (toasts.length === 0) {
    return null;
  }

  return (
    <div className="toast-container">
      {toasts.map((toast) => (
        <div className="toast" key={toast.id}>
          <div>
            <strong>{notificationTitle(toast.type)}</strong>
            <p>{toast.message}</p>
          </div>
          <button type="button" aria-label="Dismiss" onClick={() => dismissToast(toast.id)}>
            ×
          </button>
        </div>
      ))}
    </div>
  );
}
