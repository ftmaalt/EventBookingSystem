import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";

const provider = vi.hoisted(() => vi.fn(({ children }) => <>{children}</>));
vi.mock("@vis.gl/react-google-maps", () => ({
  APIProvider: provider,
  APILoadingStatus: { LOADED: "LOADED", FAILED: "FAILED", AUTH_FAILURE: "AUTH_FAILURE" },
  useApiLoadingStatus: () => "LOADED",
  Map: ({ children }) => <div data-testid="map">{children}</div>,
  Marker: () => null,
  AdvancedMarker: () => null,
  useMap: () => null,
}));

beforeEach(() => {
  vi.resetModules();
  provider.mockClear();
  vi.stubEnv("VITE_GOOGLE_MAPS_API_KEY", "test-browser-key");
});
afterEach(() => { cleanup(); vi.unstubAllEnvs(); });

it("does not load Google when coordinates are missing or invalid", async () => {
  const { default: LocationMap } = await import("../src/components/LocationMap");
  render(<LocationMap location={{ name: "Center", latitude: 91, longitude: 0 }} />);
  expect(provider).not.toHaveBeenCalled();
  expect(screen.getByRole("status").textContent).toContain("coordinates are unavailable");
});

it("keeps the missing-key fallback and directions without starting a loader", async () => {
  vi.stubEnv("VITE_GOOGLE_MAPS_API_KEY", "");
  const { default: LocationMap } = await import("../src/components/LocationMap");
  render(<LocationMap location={{ name: "Center", latitude: 26.2, longitude: 50.5 }} />);
  expect(provider).not.toHaveBeenCalled();
  expect(screen.getByRole("status").textContent).toContain("not configured");
  expect(screen.getByRole("link").href).toContain("destination=26.2%2C50.5");
});

it("loads valid previews, including zero coordinates, and updates coordinate-based directions", async () => {
  const { default: LocationMap } = await import("../src/components/LocationMap");
  const view = render(<LocationMap location={{ latitude: 0, longitude: 0 }} />);
  expect(screen.getByTestId("map")).toBeTruthy();
  view.rerender(<LocationMap location={{ latitude: 26.2, longitude: 50.5, googlePlaceId: null }} />);
  expect(screen.getByTestId("map")).toBeTruthy();
  expect(provider.mock.calls[0][0].apiKey).toBe("test-browser-key");
  expect(screen.getByRole("link").href).not.toContain("destination_place_id");
});
