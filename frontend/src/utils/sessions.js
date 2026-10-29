/** True when a session can still be booked. */
export function isBookable(session) {
  return session.status === "SCHEDULED" && session.spotsLeft > 0 && new Date(session.startTime) > new Date();
}

/** A full session can still have SCHEDULED status until its status is refreshed. */
export function isWaitlistable(session) {
  return ["SCHEDULED", "FULL"].includes(session.status) && session.spotsLeft === 0 &&
    new Date(session.startTime) > new Date();
}
