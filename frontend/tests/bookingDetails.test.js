import { expect, it, vi } from "vitest";
import { createBookingDetailsLoader } from "../src/utils/bookingDetails";

function setup() {
  const loadSession = vi.fn(async (id) => ({ data: { id, activityId: 10 } }));
  const loadActivity = vi.fn(async (id) => ({ data: { activity_id: id, title: "Experience" } }));
  return { loadSession, loadActivity, load: createBookingDetailsLoader({ loadSession, loadActivity }) };
}

it("loads each session once and shares an activity request across different sessions", async () => {
  const { load, loadSession, loadActivity } = setup();
  const details = await load([{ sessionId: 1 }, { sessionId: 1 }, { sessionId: 2 }]);
  expect(loadSession).toHaveBeenCalledTimes(2);
  expect(loadActivity).toHaveBeenCalledOnce();
  expect(details[1].activity.activity_id).toBe(10);
  expect(details[2].session.id).toBe(2);
});

it("reuses pending and completed requests across overlapping booking-page loads", async () => {
  const { load, loadSession, loadActivity } = setup();
  await Promise.all([load([{ sessionId: 1 }]), load([{ sessionId: 1 }, { sessionId: 2 }])]);
  await load([{ sessionId: 2 }]);
  expect(loadSession).toHaveBeenCalledTimes(2);
  expect(loadActivity).toHaveBeenCalledOnce();
});

it("retries failed activity requests without refetching a successful session", async () => {
  const { load, loadSession, loadActivity } = setup();
  loadActivity.mockRejectedValueOnce(new Error("Offline"));
  expect((await load([{ sessionId: 1 }]))[1]).toBeNull();
  expect((await load([{ sessionId: 1 }]))[1].activity.title).toBe("Experience");
  expect(loadSession).toHaveBeenCalledOnce();
  expect(loadActivity).toHaveBeenCalledTimes(2);
});

it("keeps failed sessions retryable and does not share caches between page instances", async () => {
  const { load, loadSession, loadActivity } = setup();
  loadSession.mockRejectedValueOnce(new Error("Offline"));
  expect((await load([{ sessionId: 1 }]))[1]).toBeNull();
  expect((await load([{ sessionId: 1 }]))[1].session.id).toBe(1);
  const secondPage = createBookingDetailsLoader({ loadSession, loadActivity });
  await secondPage([{ sessionId: 1 }]);
  expect(loadSession).toHaveBeenCalledTimes(3);
  expect(loadActivity).toHaveBeenCalledTimes(2);
});
