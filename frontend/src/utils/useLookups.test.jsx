import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { locationsApi } from "../api";
import { useLookups } from "./useLookups";

vi.mock("../api", () => ({
  categoriesApi: { getAll: vi.fn(async () => ({ data: [] })) },
  locationsApi: { getAll: vi.fn() },
}));
const original = { id: 10, latitude: 26.2, longitude: 50.5 };
const saved = { ...original, latitude: 26.3, longitude: 50.6, googlePlaceId: null };
let connections;

beforeEach(() => {
  connections = [];
  vi.stubGlobal("BroadcastChannel", class {
    constructor() { connections.push(this); }
    addEventListener(type, listener) { this.receive = listener; }
    postMessage = vi.fn();
    close = vi.fn();
  });
  locationsApi.getAll.mockReset().mockResolvedValue({ data: [original] });
});
afterEach(() => { cleanup(); vi.unstubAllGlobals(); });

it("does not let an older in-flight list response overwrite a successful pin save", async () => {
  let finishRead;
  locationsApi.getAll.mockReturnValue(new Promise((resolve) => { finishRead = resolve; }));
  const { result } = renderHook(() => useLookups());
  act(() => result.current.upsertLocation(saved));
  await act(async () => finishRead({ data: [original] }));
  expect(result.current.locationById(10).latitude).toBe(26.3);
  expect(locationsApi.getAll).toHaveBeenCalledOnce();
});

it("receives another tab's saved coordinates without fetching or saving again", async () => {
  const { result } = renderHook(() => useLookups());
  await waitFor(() => expect(result.current.locations).toHaveLength(1));
  act(() => connections[0].receive({ data: saved }));
  expect(result.current.locationById(10).latitude).toBe(26.3);
  expect(locationsApi.getAll).toHaveBeenCalledOnce();
  expect(connections[0].postMessage).not.toHaveBeenCalled();
});

it("shares one cross-tab channel between consumers and closes it after the last unmount", async () => {
  const first = renderHook(() => useLookups());
  const second = renderHook(() => useLookups());
  await waitFor(() => expect(second.result.current.locations).toHaveLength(1));
  expect(connections).toHaveLength(1);
  act(() => first.result.current.upsertLocation(saved));
  expect(second.result.current.locationById(10).latitude).toBe(26.3);
  expect(connections[0].postMessage).toHaveBeenCalledExactlyOnceWith(saved);
  first.unmount();
  expect(connections[0].close).not.toHaveBeenCalled();
  second.unmount();
  expect(connections[0].close).toHaveBeenCalledOnce();
});
