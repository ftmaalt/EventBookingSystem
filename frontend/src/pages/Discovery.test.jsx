import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from "react-router-dom";
import { activitiesApi } from "../api";
import Landing from "./Landing";
import Activities from "./Activities";

vi.mock("../auth", () => ({ useAuth: () => ({ token: null }) }));
vi.mock("../components/Layout", () => ({ default: ({ children }) => <>{children}</> }));
vi.mock("../utils/useLookups", () => ({ useLookups: () => ({
  categories: [{ category_id: 3, category_name: "Creative workshops" }],
  locations: [{ id: 9, name: "Manama studio" }],
  categoryById: () => ({ category_name: "Creative workshops" }),
  locationById: () => ({ name: "Manama studio" }),
}) }));
vi.mock("../api", () => ({ activitiesApi: { getAll: vi.fn() } }));

const activity = { activity_id: 21, title: "Ceramic workshop", description: "Shape a bowl with clay.",
  status: "ACTIVE", pricePerPerson: 15, durationMinutes: 90, categoryId: 3, locationId: 9 };
function Destination() { const location = useLocation(); return <output>{location.pathname}{location.search}</output>; }
beforeEach(() => { activitiesApi.getAll.mockReset().mockResolvedValue({ data: { content: [activity], totalPages: 1 } }); });
afterEach(cleanup);

it("keeps real category navigation and the adventure quote without the removed homepage sections", () => {
  const { container } = render(<MemoryRouter><Landing /></MemoryRouter>);
  expect(screen.getByText("Creative workshops", { selector: ".category-card > span:not(.category-card-symbol)" }).closest("a").getAttribute("href")).toBe("/activities?category=3");
  expect(screen.getByText("Eleanor Roosevelt")).toBeTruthy();
  expect(screen.getByRole("blockquote").textContent).toContain("The purpose of life is to live it");
  expect(screen.queryByText("The Social Edit")).toBeNull();
  expect(screen.queryByText("More to explore")).toBeNull();
  expect(screen.queryByRole("article")).toBeNull();
  expect(activitiesApi.getAll).not.toHaveBeenCalled();
  expect(container.querySelector("img")).toBeNull();
});

it("homepage search navigates to the existing discovery route with an encoded query", () => {
  render(<MemoryRouter><Routes><Route path="/" element={<Landing />} /><Route path="/activities" element={<Destination />} /></Routes></MemoryRouter>);
  fireEvent.change(screen.getByRole("searchbox"), { target: { value: "arts & clay" } });
  fireEvent.submit(screen.getByRole("search"));
  expect(screen.getByText("/activities?q=arts%20%26%20clay")).toBeTruthy();
});

it("keeps a clear API failure state on the experience discovery page", async () => {
  activitiesApi.getAll.mockRejectedValue({ userMessage: "Cannot reach the server." });
  render(<MemoryRouter><Activities /></MemoryRouter>);
  await screen.findByText("Cannot reach the server.");
  expect(screen.queryByRole("article")).toBeNull();
});

it("applies category/query navigation and accepts a new navigation search while already exploring", async () => {
  function SearchAgain() {
    const navigate = useNavigate();
    return <><button onClick={() => navigate("/activities?q=another")}>New navigation search</button><Activities /></>;
  }
  render(<MemoryRouter initialEntries={["/activities?category=3&q=ceramic"]}><SearchAgain /></MemoryRouter>);
  await screen.findByRole("link", { name: "Ceramic workshop" });
  expect(screen.getByLabelText("What inspires you?").value).toBe("ceramic");
  expect(screen.getByLabelText("Your kind of experience").value).toBe("3");
  expect(activitiesApi.getAll).toHaveBeenCalledWith(expect.objectContaining({ categoryId: "3", size: 100 }));
  fireEvent.click(screen.getByText("New navigation search"));
  await waitFor(() => expect(screen.getByLabelText("What inspires you?").value).toBe("another"));
  expect(screen.getByLabelText("Your kind of experience").value).toBe("");
  await screen.findByText("No activities found");
});
