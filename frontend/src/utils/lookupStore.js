const CACHE_MS = 30_000;

function mergeLocation(items, saved) {
  return items.some((item) => item.id === saved.id)
    ? items.map((item) => item.id === saved.id ? saved : item)
    : [...items, saved];
}

export function openLocationChannel(onLocation) {
  try {
    if (typeof BroadcastChannel === "undefined") return null;
    const channel = new BroadcastChannel("bookngo-locations");
    channel.addEventListener("message", ({ data }) => {
      if (data?.id != null) onLocation(data);
    });
    return channel;
  } catch {
    return null;
  }
}

/** Shared public reference data; concurrent consumers reuse one refresh. */
export function createLookupStore({ loadCategories, loadLocations, connect = openLocationChannel, now = Date.now }) {
  let snapshot = { categories: [], locations: [] };
  let inFlight = null;
  let loadedAt = null;
  let channel = null;
  let revision = 0;
  const recentWrites = new Map();
  const listeners = new Set();

  const publish = (changes) => {
    snapshot = { ...snapshot, ...changes };
    listeners.forEach((listener) => listener());
  };

  const applyLocation = (saved) => {
    if (saved?.id == null) return;
    recentWrites.set(saved.id, { revision: ++revision, saved });
    publish({ locations: mergeLocation(snapshot.locations, saved) });
  };

  const reload = () => {
    if (inFlight) return inFlight;
    const startedAt = revision;
    inFlight = Promise.allSettled([loadCategories(), loadLocations()])
      .then(([categories, locations]) => {
        const changes = {};
        if (categories.status === "fulfilled") changes.categories = categories.value.data || [];
        if (locations.status === "fulfilled") {
          let next = locations.value.data || [];
          // A response started before a pin save must not undo the saved position.
          for (const write of recentWrites.values()) {
            if (write.revision > startedAt) next = mergeLocation(next, write.saved);
          }
          recentWrites.clear();
          changes.locations = next;
        }
        loadedAt = categories.status === "fulfilled" && locations.status === "fulfilled" ? now() : null;
        if (Object.keys(changes).length) publish(changes);
      })
      .finally(() => { inFlight = null; });
    return inFlight;
  };

  return {
    getSnapshot: () => snapshot,
    subscribe(listener) {
      if (listeners.size === 0) channel = connect(applyLocation);
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
        if (listeners.size === 0) {
          channel?.close();
          channel = null;
        }
      };
    },
    reload,
    ensureLoaded: () => loadedAt !== null && now() - loadedAt < CACHE_MS ? Promise.resolve() : reload(),
    upsertLocation(saved) {
      applyLocation(saved);
      try { channel?.postMessage(saved); } catch { /* Local previews already have the saved response. */ }
    },
  };
}
