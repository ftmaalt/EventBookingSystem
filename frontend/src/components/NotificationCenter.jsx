import { useState } from "react";

import { notificationsApi } from "../api";
import { useAuth } from "../auth";
import { formatDate, notificationTitle } from "../utils/format";

/** Bell icon with the notification dropdown. */
export default function NotificationCenter() {
  const { notifications, setNotifications, refreshNotifications } = useAuth();
  const [open, setOpen] = useState(false);

  const unreadCount = notifications.filter((item) => !item.read).length;

  const toggle = async () => {
    const nextOpen = !open;
    setOpen(nextOpen);
    if (!nextOpen) {
      return;
    }

    const latest = await refreshNotifications();
    if (latest.some((item) => !item.read)) {
      setNotifications((items) => items.map((item) => ({ ...item, read: true })));
      try {
        await notificationsApi.markAllRead();
      } catch (error) {
        console.error("Could not mark notifications as read.", error);
      }
    }
  };

  const clearAll = async () => {
    const previous = notifications;
    setNotifications([]);
    try {
      await notificationsApi.clearAll();
    } catch (error) {
      setNotifications(previous);
      console.error("Could not clear notifications.", error);
    }
  };

  const remove = async (id) => {
    const previous = notifications;
    setNotifications((items) => items.filter((item) => item.id !== id));
    try {
      await notificationsApi.remove(id);
    } catch (error) {
      setNotifications(previous);
      console.error("Could not remove notification.", error);
    }
  };

  return (
    <div className="notification-center">
      <button type="button" className="notification-bell" onClick={toggle} aria-label="Notifications" aria-expanded={open}>
        <span aria-hidden="true">🔔</span>
        {unreadCount > 0 && <span className="notification-badge">{unreadCount > 9 ? "9+" : unreadCount}</span>}
      </button>

      {open && (
        <div className="notification-panel">
          <div className="notification-panel-header">
            <div>
              <strong>Notifications</strong>
              <span>Recent BookNGo updates</span>
            </div>
            {notifications.length > 0 && (
              <button type="button" onClick={clearAll}>
                Clear all
              </button>
            )}
          </div>

          <div className="notification-panel-list">
            {notifications.length === 0 ? (
              <div className="notification-empty">
                <span>✓</span>
                <p>No notifications yet.</p>
              </div>
            ) : (
              notifications.map((notification) => (
                <div className="notification-item" key={notification.id}>
                  <span className="notification-dot" />
                  <div>
                    <strong>{notificationTitle(notification.type)}</strong>
                    <p>{notification.message}</p>
                    {notification.createdAt && <small>{formatDate(notification.createdAt)}</small>}
                  </div>
                  <button
                    type="button"
                    className="notification-remove"
                    aria-label="Remove notification"
                    onClick={() => remove(notification.id)}
                  >
                    ×
                  </button>
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
