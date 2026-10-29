import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { waitlistApi } from "../src/api";
import SessionCard from "../src/components/SessionCard";

const auth = vi.hoisted(() => ({ token: "test-token", role: "USER", user: { id: 7, status: "ACTIVE" } }));
vi.mock("../src/auth", () => ({ useAuth: () => auth }));
vi.mock("../src/api", () => ({ waitlistApi: { getMine: vi.fn(), join: vi.fn(), leave: vi.fn() } }));
const noEntry = { response: { status: 404 } };
const entry = { entryId: 12, sessionId: 2, status: "WAITING", position: 3 };
const fullSession = () => ({ id: 2, spotsLeft: 0, capacity: 5, status: "SCHEDULED",
  startTime: new Date(Date.now() + 86400000).toISOString(), endTime: new Date(Date.now() + 90000000).toISOString() });
function Destination() { const location = useLocation(); return <output>{location.state?.from}</output>; }
function open(session = fullSession(), options = {}) {
  return render(<MemoryRouter><SessionCard session={session} activityActive returnTo="/activities/21" {...options} /><Destination /></MemoryRouter>);
}
beforeEach(() => {
  vi.resetAllMocks();
  auth.token = "test-token"; auth.role = "USER"; auth.user = { id: 7, status: "ACTIVE" };
  waitlistApi.getMine.mockRejectedValue(noEntry);
  waitlistApi.join.mockResolvedValue({ data: entry });
  waitlistApi.leave.mockResolvedValue({ data: "You have successfully left the waitlist." });
});
afterEach(cleanup);

it("joins a full SCHEDULED session and links to Waitlisted without showing management controls here", async () => {
  open();
  const join = await screen.findByRole("button", { name: "Join waitlist" });
  fireEvent.click(join);
  const link = await screen.findByRole("link", { name: "View waitlist" });
  expect(waitlistApi.join).toHaveBeenCalledExactlyOnceWith(2);
  expect(link.getAttribute("href")).toBe("/bookings?tab=waitlisted");
  expect(screen.queryByText(/Position/)).toBeNull();
  expect(screen.queryByRole("button", { name: "Leave waitlist" })).toBeNull();
  expect(screen.queryByRole("button", { name: "Refresh position" })).toBeNull();
});

it("restores membership after a page reload and directs management to Bookings", async () => {
  waitlistApi.getMine.mockResolvedValueOnce({ data: entry }).mockResolvedValueOnce({ data: { ...entry, position: 1 } });
  open({ ...fullSession(), status: "FULL" });
  await screen.findByRole("link", { name: "View waitlist" });
  expect(screen.queryByRole("button", { name: "Join waitlist" })).toBeNull();
  expect(screen.queryByText(/Position/)).toBeNull();
});

it("requires login for guests and preserves the activity return destination", () => {
  auth.token = null; auth.role = null; auth.user = null;
  open();
  fireEvent.click(screen.getByRole("link", { name: "Login to join waitlist" }));
  expect(screen.getByText("/activities/21", { selector: "output" })).toBeTruthy();
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
});

it.each(["PROVIDER", "ADMIN"])("does not offer customer waitlist operations to %s accounts", (role) => {
  auth.role = role;
  open();
  expect(screen.getByRole("button", { name: "Unavailable" }).disabled).toBe(true);
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
});

it("does not offer joining to inactive customers", () => {
  auth.user.status = "INACTIVE";
  open();
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
  expect(screen.queryByRole("button", { name: "Join waitlist" })).toBeNull();
});

it.each(["CANCELLED", "COMPLETED", "IN_PROGRESS"])("keeps %s sessions unavailable", (status) => {
  open({ ...fullSession(), status });
  expect(screen.getByRole("button", { name: "Unavailable" }).disabled).toBe(true);
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
});

it("does not offer a waitlist for past sessions or inactive activities", () => {
  const first = open({ ...fullSession(), startTime: new Date(Date.now() - 1000).toISOString() });
  expect(screen.getByRole("button", { name: "Unavailable" })).toBeTruthy();
  first.unmount();
  open(fullSession(), { activityActive: false });
  expect(screen.getByRole("button", { name: "Unavailable" })).toBeTruthy();
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
});

it("preserves direct booking when spots are available", () => {
  open({ ...fullSession(), spotsLeft: 3 });
  expect(screen.getByRole("link", { name: "Book Now" }).getAttribute("href")).toBe("/book/2");
  expect(waitlistApi.getMine).not.toHaveBeenCalled();
});

it("prevents duplicate join requests and displays API errors", async () => {
  let reject;
  waitlistApi.join.mockReturnValue(new Promise((resolve, fail) => { reject = fail; }));
  open();
  const join = await screen.findByRole("button", { name: "Join waitlist" });
  fireEvent.click(join);
  fireEvent.click(join);
  expect(screen.getByRole("button", { name: "Joining…" }).disabled).toBe(true);
  expect(waitlistApi.join).toHaveBeenCalledOnce();
  reject({ userMessage: "You already have an active booking for this session." });
  await screen.findByRole("alert");
  expect(screen.getByRole("alert").textContent).toContain("active booking");
});

it("offers retry when checking existing membership fails", async () => {
  waitlistApi.getMine.mockRejectedValueOnce({ userMessage: "Server unavailable." }).mockRejectedValueOnce(noEntry);
  open();
  fireEvent.click(await screen.findByRole("button", { name: "Retry waitlist status" }));
  await screen.findByRole("button", { name: "Join waitlist" });
  expect(waitlistApi.getMine).toHaveBeenCalledTimes(2);
});

it("reconciles an already-joined conflict instead of offering a duplicate join", async () => {
  waitlistApi.getMine.mockRejectedValueOnce(noEntry).mockResolvedValueOnce({ data: entry });
  waitlistApi.join.mockRejectedValue({ response: { status: 409 }, userMessage: "Already on waitlist." });
  const onSessionRefresh = vi.fn();
  open(fullSession(), { onSessionRefresh });
  fireEvent.click(await screen.findByRole("button", { name: "Join waitlist" }));
  await screen.findByRole("link", { name: "View waitlist" });
  await waitFor(() => expect(onSessionRefresh).toHaveBeenCalledOnce());
});
