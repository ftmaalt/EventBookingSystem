import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";

const maps = vi.hoisted(() => ({ mapProps: null, markerProps: null, advancedProps: null, panTo: vi.fn() }));
vi.mock("@vis.gl/react-google-maps", () => ({
  APIProvider: ({ children }) => children,
  APILoadingStatus: { LOADED: "LOADED", FAILED: "FAILED", AUTH_FAILURE: "AUTH_FAILURE" },
  useApiLoadingStatus: () => "LOADED",
  useMap: () => ({ panTo: maps.panTo }),
  Map: ({ children, ...props }) => { maps.mapProps = props; return <div>{children}</div>; },
  Marker: (props) => {
    maps.markerProps = props;
    return <button onClick={() => props.onDragEnd?.({ latLng: { toJSON: () => ({ lat: 26.3, lng: 50.6 }) } })}>Finish standard drag</button>;
  },
  AdvancedMarker: (props) => {
    maps.advancedProps = props;
    return <button onClick={() => props.onDragEnd?.({ latLng: { toJSON: () => ({ lat: 26.3, lng: 50.6 }) } })}>Finish advanced drag</button>;
  },
}));

const location = { id: 10, name: "Center", latitude: 26.2, longitude: 50.5 };
beforeEach(() => {
  vi.resetModules();
  vi.stubEnv("VITE_GOOGLE_MAPS_API_KEY", "browser-test-key");
  vi.stubEnv("VITE_GOOGLE_MAPS_MAP_ID", "");
  maps.markerProps = maps.advancedProps = maps.mapProps = null;
});
afterEach(() => { cleanup(); vi.unstubAllEnvs(); });

it("supports dragging, zoom and map-type controls without requiring a new map ID", async () => {
  const { default: LocationMap } = await import("./LocationMap");
  const onPositionChange = vi.fn();
  render(<LocationMap location={location} editable onPositionChange={onPositionChange} />);
  expect(maps.markerProps.draggable).toBe(true);
  expect(maps.mapProps.mapTypeControl).toBe(true);
  expect(maps.mapProps.zoomControl).toBe(true);
  expect(maps.mapProps.defaultZoom).toBe(18);
  fireEvent.click(screen.getByText("Finish standard drag"));
  expect(onPositionChange).toHaveBeenCalledWith({ lat: 26.3, lng: 50.6 });
});

it("uses AdvancedMarker when a map ID is configured and blocks dragging during a save", async () => {
  vi.stubEnv("VITE_GOOGLE_MAPS_MAP_ID", "configured-map-id");
  const { default: LocationMap } = await import("./LocationMap");
  const onPositionChange = vi.fn();
  render(<LocationMap location={location} editable disabled onPositionChange={onPositionChange} />);
  expect(maps.mapProps.mapId).toBe("configured-map-id");
  expect(maps.advancedProps.draggable).toBe(false);
  expect(maps.markerProps).toBeNull();
  fireEvent.click(screen.getByText("Finish advanced drag"));
  expect(onPositionChange).not.toHaveBeenCalled();
});

it("keeps customer-facing markers read-only and directions coordinate-based after manual edits", async () => {
  const { default: LocationMap } = await import("./LocationMap");
  render(<LocationMap location={{ ...location, googlePlaceId: null }} />);
  expect(maps.markerProps.draggable).toBe(false);
  expect(maps.mapProps.mapTypeControl).toBe(false);
  const url = new URL(screen.getByRole("link").href);
  expect(url.searchParams.get("destination")).toBe("26.2,50.5");
  expect(url.searchParams.has("destination_place_id")).toBe(false);
});
