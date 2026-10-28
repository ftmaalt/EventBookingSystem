import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { locationsApi } from "../api";
import Provider from "./Provider";
import ActivityDetails from "./ActivityDetails";

const fixtures = vi.hoisted(() => ({ locations: [] }));
vi.mock("../auth", () => ({ useAuth: () => ({ user: { id: 7 }, role: "PROVIDER", token: "test-token" }) }));
vi.mock("../api", () => ({
  categoriesApi: { getAll: vi.fn(async () => ({ data: [] })) },
  locationsApi: {
    getAll: vi.fn(async () => ({ data: [...fixtures.locations] })),
    create: vi.fn(), update: vi.fn(), updatePin: vi.fn(),
  },
  activitiesApi: {
    getAll: vi.fn(async () => ({ data: { content: [] } })),
    getById: vi.fn(async () => ({ data: { activity_id: 1, locationId: 10, title: "Experience", pricePerPerson: 10 } })),
  },
  sessionsApi: { getByActivity: vi.fn(async () => ({ data: [] })) },
  providerApi: {},
}));
vi.mock("../components/Layout", () => ({ default: ({ children }) => <div>{children}</div> }));
vi.mock("../components/ViolationForm", () => ({ default: () => null }));
vi.mock("../components/LocationMap", async (importOriginal) => ({
  ...await importOriginal(),
  default: ({ location, editable, onPositionChange }) => <div data-testid={editable ? "editor-map" : "saved-map"}
    data-latitude={location?.latitude} data-longitude={location?.longitude}>
    {editable && <button type="button" onClick={() => onPositionChange({ lat: 26.24, lng: 50.58 })}>Drag pin</button>}
  </div>,
}));

const ownerLocation = { id: 10, name: "Own center", address: "Road 1", city: "Manama", providerId: 7,
  latitude: 26.2, longitude: 50.5, googlePlaceId: "old-place" };

async function openLocations() {
  const view = render(<MemoryRouter><Provider /></MemoryRouter>);
  fireEvent.click(screen.getByText("Locations", { selector: "button" }));
  await screen.findByText("Own center", { selector: "strong" });
  return view;
}

beforeEach(() => {
  vi.clearAllMocks();
  fixtures.locations = [ownerLocation, { ...ownerLocation, id: 11, name: "Other center", providerId: 8 },
    { ...ownerLocation, id: 12, name: "Unknown owner", providerId: null }];
  locationsApi.updatePin.mockImplementation(async (id, coordinates) => {
    const saved = { ...fixtures.locations.find((item) => item.id === id), ...coordinates, googlePlaceId: null };
    fixtures.locations = fixtures.locations.map((item) => item.id === id ? saved : item);
    return { data: saved };
  });
});
afterEach(cleanup);

it("offers pin editing only for an owned location", async () => {
  await openLocations();
  expect(screen.getAllByText("Edit Pin Location")).toHaveLength(1);
  expect(screen.getByText("Other center").closest(".application").textContent).not.toContain("Edit Pin Location");
  expect(screen.getByText("Unknown owner").closest(".application").textContent).not.toContain("Edit Pin Location");
});

it("Cancel restores the saved preview and reopening starts at the original pin", async () => {
  await openLocations();
  fireEvent.click(screen.getByText("Edit Pin Location"));
  fireEvent.click(screen.getByText("Drag pin"));
  fireEvent.click(screen.getByText("Cancel", { selector: "button" }));
  expect(screen.getByTestId("saved-map").dataset.latitude).toBe("26.2");
  expect(locationsApi.updatePin).not.toHaveBeenCalled();
  fireEvent.click(screen.getByText("Edit Pin Location"));
  expect(screen.getByTestId("editor-map").dataset.latitude).toBe("26.2");
});

it("refreshes provider and already-open activity maps, then reloads saved coordinates from the API", async () => {
  const provider = await openLocations();
  const activity = render(<MemoryRouter initialEntries={["/activities/1"]}><Routes>
    <Route path="/activities/:id" element={<ActivityDetails />} />
  </Routes></MemoryRouter>);
  await waitFor(() => expect(within(activity.container).getByTestId("saved-map").dataset.latitude).toBe("26.2"));
  fireEvent.click(screen.getByText("Edit Pin Location"));
  fireEvent.click(screen.getByText("Drag pin"));
  fireEvent.click(screen.getByText("Save Pin Location"));
  await screen.findByText("Pin location saved.");
  expect(within(provider.container).getByTestId("saved-map").dataset.latitude).toBe("26.24");
  expect(within(activity.container).getByTestId("saved-map").dataset.latitude).toBe("26.24");
  expect(fixtures.locations[0].googlePlaceId).toBeNull();
  provider.unmount();
  activity.unmount();
  await openLocations();
  await waitFor(() => expect(screen.getByTestId("saved-map").dataset.latitude).toBe("26.24"));
  expect(locationsApi.updatePin).toHaveBeenCalledOnce();
});
