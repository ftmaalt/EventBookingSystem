/** True when a session can still be booked. */
export function isBookable(session) {
  return session.status === "SCHEDULED" && session.spotsLeft > 0 && new Date(session.startTime) > new Date();
}
