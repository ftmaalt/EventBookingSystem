import { describe, expect, it, vi } from "vitest";
import { createLookupStore } from "../src/utils/lookupStore";

const response = (data) => ({ data });
const original = { id: 10, latitude: 26.2, longitude: 50.5 };
const saved = { ...original, latitude: 26.3, longitude: 50.6, googlePlaceId: null };

function setup(options = {}) {
  const loadCategories = vi.fn(async () => response([{ category_id: 3, category_name: "Arts" }]));
  const loadLocations = vi.fn(async () => response([original]));
  const store = createLookupStore({ loadCategories, loadLocations, connect: () => null, ...options });
  return { store, loadCategories, loadLocations };
}

describe("shared reference data", () => {
  it("deduplicates concurrent refreshes and serves fresh data without another request", async () => {
    const { store, loadCategories, loadLocations } = setup();
    const first = store.ensureLoaded();
    expect(store.ensureLoaded()).toBe(first);
    expect(store.reload()).toBe(first);
    await first;
    await store.ensureLoaded();
    expect(loadCategories).toHaveBeenCalledOnce();
    expect(loadLocations).toHaveBeenCalledOnce();
    expect(store.getSnapshot().locations).toEqual([original]);
  });

  it("refreshes after thirty seconds and allows an immediate explicit refresh", async () => {
    let clock = 0;
    const { store, loadLocations } = setup({ now: () => clock });
    await store.ensureLoaded();
    clock = 29_999;
    await store.ensureLoaded();
    expect(loadLocations).toHaveBeenCalledOnce();
    clock = 30_000;
    await store.ensureLoaded();
    await store.reload();
    expect(loadLocations).toHaveBeenCalledTimes(3);
  });

  it("does not let an older list response overwrite a newer pin save", async () => {
    let finish;
    const { store } = setup({ loadLocations: () => new Promise((resolve) => { finish = resolve; }) });
    const refresh = store.reload();
    store.upsertLocation(saved);
    finish(response([original]));
    await refresh;
    expect(store.getSnapshot().locations).toEqual([saved]);
  });

  it("preserves successful data during partial failures and retries on the next mount", async () => {
    const { store, loadLocations } = setup();
    await store.reload();
    loadLocations.mockRejectedValueOnce(new Error("Offline"));
    await store.reload();
    expect(store.getSnapshot().locations).toEqual([original]);
    await store.ensureLoaded();
    expect(loadLocations).toHaveBeenCalledTimes(3);
  });

  it("shares one channel, broadcasts saved locations, and does not relay received messages", () => {
    let receive;
    const channel = { postMessage: vi.fn(), close: vi.fn() };
    const connect = vi.fn((listener) => { receive = listener; return channel; });
    const { store } = setup({ connect });
    const first = vi.fn();
    const second = vi.fn();
    const unsubscribeFirst = store.subscribe(first);
    const unsubscribeSecond = store.subscribe(second);
    expect(connect).toHaveBeenCalledOnce();
    store.upsertLocation(saved);
    expect(first).toHaveBeenCalledOnce();
    expect(second).toHaveBeenCalledOnce();
    expect(channel.postMessage).toHaveBeenCalledExactlyOnceWith(saved);
    receive({ ...saved, latitude: 26.4 });
    expect(store.getSnapshot().locations[0].latitude).toBe(26.4);
    expect(channel.postMessage).toHaveBeenCalledOnce();
    unsubscribeFirst();
    expect(channel.close).not.toHaveBeenCalled();
    unsubscribeSecond();
    expect(channel.close).toHaveBeenCalledOnce();
  });

  it("keeps local saves working when browser channel writes are unavailable", () => {
    const { store } = setup({ connect: () => ({ close() {}, postMessage() { throw new Error("Blocked"); } }) });
    const unsubscribe = store.subscribe(() => {});
    expect(() => store.upsertLocation(saved)).not.toThrow();
    expect(store.getSnapshot().locations).toEqual([saved]);
    unsubscribe();
  });
});
