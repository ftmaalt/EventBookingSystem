import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { bookingsApi, waitlistApi } from "../src/api";
import Bookings from "../src/pages/Bookings";

vi.mock("../src/components/Layout", () => ({ default: ({ children }) => <>{children}</> }));
vi.mock("../src/api", () => ({
  waitlistApi: { getAll: vi.fn(), leave: vi.fn() },
  bookingsApi: { getAll: vi.fn(), cancel: vi.fn() },
  sessionsApi: { getById: vi.fn() }, activitiesApi: { getById: vi.fn() },
}));
const entry = { entryId: 12, sessionId: 2, activityId: 21, activityTitle: "Ceramic workshop", position: 3,
  startTime: "2027-01-01T15:00:00", endTime: "2027-01-01T16:00:00", joinedAt: "2026-10-29T10:00:00" };
const response = (content) => ({ data: { content, totalPages: content.length ? 1 : 0, totalElements: content.length } });
function open(path = "/bookings?tab=waitlisted") {
  return render(<MemoryRouter initialEntries={[path]}><Bookings /></MemoryRouter>);
}
beforeEach(() => {
  vi.resetAllMocks();
  bookingsApi.getAll.mockResolvedValue(response([]));
  waitlistApi.getAll.mockResolvedValue(response([entry]));
  waitlistApi.leave.mockResolvedValue({ data: "You have successfully left the waitlist." });
});
afterEach(cleanup);

it("opens the persisted Waitlisted tab directly without requesting a fake booking status", async () => {
  open();
  const activity = await screen.findByRole("link", { name: "Ceramic workshop" });
  expect(activity.getAttribute("href")).toBe("/activities/21");
  expect(screen.getByText("Position 3")).toBeTruthy();
  expect(screen.getByRole("button", { name: "WAITLISTED" }).className).toBe("active");
  expect(bookingsApi.getAll).not.toHaveBeenCalled();
  expect(waitlistApi.getAll).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 10 }), expect.any(Object));
});

it("offers waitlist management from the new tab and returns to normal booking filters", async () => {
  open("/bookings");
  await screen.findByText("No bookings");
  fireEvent.click(screen.getByRole("button", { name: "WAITLISTED" }));
  await screen.findByText("Position 3");
  fireEvent.click(screen.getByRole("button", { name: "CONFIRMED" }));
  await screen.findByText("No bookings");
  expect(bookingsApi.getAll).toHaveBeenLastCalledWith(expect.objectContaining({ status: "CONFIRMED" }));
  expect(screen.queryByText("Position 3")).toBeNull();
});

it("refreshes queue positions in Bookings", async () => {
  waitlistApi.getAll.mockResolvedValueOnce(response([entry])).mockResolvedValueOnce(response([{ ...entry, position: 1 }]));
  open();
  await screen.findByText("Position 3");
  fireEvent.click(screen.getByRole("button", { name: "Refresh positions" }));
  await screen.findByText("Position 1");
});

it("leaves the waitlist using the session ID and refreshes the list to an empty state", async () => {
  waitlistApi.getAll.mockResolvedValueOnce(response([entry])).mockResolvedValueOnce(response([]));
  open();
  fireEvent.click(await screen.findByRole("button", { name: "Leave waitlist" }));
  await screen.findByText("No waitlisted sessions");
  expect(waitlistApi.leave).toHaveBeenCalledExactlyOnceWith(2);
  expect(screen.getByRole("status").textContent).toContain("You have left the waitlist for Ceramic workshop.");
});

it("keeps errors visible and prevents duplicate leave requests", async () => {
  let reject;
  waitlistApi.leave.mockReturnValue(new Promise((resolve, fail) => { reject = fail; }));
  open();
  const leave = await screen.findByRole("button", { name: "Leave waitlist" });
  fireEvent.click(leave); fireEvent.click(leave);
  expect(waitlistApi.leave).toHaveBeenCalledOnce();
  expect(screen.getByRole("button", { name: "Refresh positions" }).disabled).toBe(true);
  reject({ userMessage: "Unable to leave right now." });
  await screen.findByRole("alert");
  expect(screen.getByText("Position 3")).toBeTruthy();
  expect(screen.getByRole("button", { name: "Leave waitlist" }).disabled).toBe(false);
});

it("allows retrying a failed list request without losing normal booking tabs", async () => {
  waitlistApi.getAll.mockRejectedValueOnce({ userMessage: "Server unavailable." }).mockResolvedValueOnce(response([entry]));
  open();
  await screen.findByRole("alert");
  fireEvent.click(screen.getByRole("button", { name: "Refresh positions" }));
  await screen.findByText("Position 3");
  await waitFor(() => expect(screen.queryByRole("alert")).toBeNull());
  expect(screen.getByRole("button", { name: "CONFIRMED" })).toBeTruthy();
});
