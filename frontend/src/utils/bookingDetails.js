/** Cache session/activity requests only for the lifetime of one bookings page. */
export function createBookingDetailsLoader({ loadSession, loadActivity }) {
  const cacheRequests = (load) => {
    const requests = new Map();
    return (id) => {
      if (!requests.has(id)) {
        const request = Promise.resolve().then(() => load(id))
          .then((response) => response.data)
          .catch((error) => {
            requests.delete(id); // A temporary failure must remain retryable.
            throw error;
          });
        requests.set(id, request);
      }
      return requests.get(id);
    };
  };

  const sessionById = cacheRequests(loadSession);
  const activityById = cacheRequests(loadActivity);

  return async (bookings) => {
    const sessionIds = [...new Set(bookings.map((booking) => booking.sessionId))];
    const entries = await Promise.all(sessionIds.map(async (id) => {
      try {
        const session = await sessionById(id);
        const activity = await activityById(session.activityId);
        return [id, { session, activity }];
      } catch {
        return [id, null];
      }
    }));
    return Object.fromEntries(entries);
  };
}
