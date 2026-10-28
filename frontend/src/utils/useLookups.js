import { useCallback, useEffect, useRef, useState } from "react";

import { categoriesApi, locationsApi } from "../api";

const LOCATION_UPDATED = "bookngo:location-updated";
// Share public saved location data with already-open activity maps, including other tabs.
let channel = null;
let subscribers = 0;

function openChannel() {
  try {
    if (typeof BroadcastChannel === "undefined") return null;
    const connection = new BroadcastChannel("bookngo-locations");
    connection.addEventListener("message", ({ data }) => {
      if (data?.id != null) window.dispatchEvent(new CustomEvent(LOCATION_UPDATED, { detail: data }));
    });
    return connection;
  } catch {
    return null; // Restricted browser storage must not prevent local saves or previews.
  }
}

function mergeLocation(items, saved) {
  return items.some((item) => item.id === saved.id)
    ? items.map((item) => item.id === saved.id ? saved : item)
    : [...items, saved];
}

/** Loads categories and locations (public reference data) and offers lookups by id. */
export function useLookups() {
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);
  const revision = useRef(0);
  const recentWrites = useRef(new Map());

  const reload = useCallback(async () => {
    const startedAt = revision.current;
    const [categoryResult, locationResult] = await Promise.allSettled([categoriesApi.getAll(), locationsApi.getAll()]);
    if (categoryResult.status === "fulfilled") {
      setCategories(categoryResult.value.data);
    }
    if (locationResult.status === "fulfilled") {
      // A list request started before a pin save must not replace the newer saved pin.
      let next = locationResult.value.data;
      for (const { version, saved } of recentWrites.current.values()) {
        if (version > startedAt) next = mergeLocation(next, saved);
      }
      setLocations(next);
    }
  }, []);

  useEffect(() => {
    if (subscribers++ === 0) channel = openChannel();
    const onUpdated = (event) => {
      recentWrites.current.set(event.detail.id, { version: ++revision.current, saved: event.detail });
      setLocations((items) => mergeLocation(items, event.detail));
    };
    window.addEventListener(LOCATION_UPDATED, onUpdated);
    reload();
    return () => {
      window.removeEventListener(LOCATION_UPDATED, onUpdated);
      if (--subscribers === 0) {
        channel?.close();
        channel = null;
      }
    };
  }, [reload]);

  const categoryById = (id) => categories.find((item) => item.category_id === id);
  const locationById = (id) => locations.find((item) => item.id === id);

  // Keep a successful write visible even if the subsequent list refresh fails.
  const upsertLocation = (saved) => {
    window.dispatchEvent(new CustomEvent(LOCATION_UPDATED, { detail: saved }));
    try { channel?.postMessage(saved); } catch { /* Local consumers already received the saved response. */ }
  };

  return { categories, locations, categoryById, locationById, reload, upsertLocation };
}
