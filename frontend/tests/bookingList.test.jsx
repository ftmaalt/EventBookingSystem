import { afterEach, expect, it, vi } from "vitest";
import { act, cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { activitiesApi, bookingsApi, sessionsApi } from "../src/api";
import Bookings from "../src/pages/Bookings";

vi.mock("../src/components/Layout", () => ({ default: ({ children }) => <>{children}</> }));
vi.mock("../src/api", () => ({
  bookingsApi: { getAll: vi.fn(), cancel: vi.fn() },
  activitiesApi: { getById: vi.fn() },
  sessionsApi: { getById: vi.fn() },
}));
afterEach(() => { cleanup(); vi.clearAllMocks(); });

it("does not let an older filter response replace the latest bookings", async () => {
  let finishOldRequest;
  bookingsApi.getAll.mockImplementation(({ status }) => status === "CONFIRMED"
    ? Promise.resolve({ data: { content: [{ id: 2, sessionId: 2, status: "CONFIRMED", participants: 1,
      bookingType: "INDIVIDUAL", paymentStatus: "PAID", totalPrice: 10 }], totalPages: 1 } })
    : new Promise((resolve) => { finishOldRequest = resolve; }));
  sessionsApi.getById.mockResolvedValue({ data: { id: 2, activityId: 10 } });
  activitiesApi.getById.mockResolvedValue({ data: { activity_id: 10, title: "Latest activity" } });
  render(<MemoryRouter><Bookings /></MemoryRouter>);
  fireEvent.click(screen.getByRole("button", { name: "CONFIRMED" }));
  await screen.findByRole("link", { name: "Latest activity" });
  await act(async () => finishOldRequest({ data: { content: [{ id: 1, sessionId: 1 }], totalPages: 9 } }));
  expect(screen.getByRole("link", { name: "Latest activity" })).toBeTruthy();
  expect(screen.queryByText("Booking #1")).toBeNull();
  expect(sessionsApi.getById).toHaveBeenCalledExactlyOnceWith(2);
});
