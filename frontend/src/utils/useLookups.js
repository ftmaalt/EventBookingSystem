import { useCallback, useEffect, useMemo, useSyncExternalStore } from "react";

import { categoriesApi, locationsApi } from "../api";
import { createLookupStore } from "./lookupStore";

const store = createLookupStore({ loadCategories: categoriesApi.getAll, loadLocations: locationsApi.getAll });

/** All pages share reference data; explicit reloads still refresh it immediately. */
export function useLookups() {
  const { categories, locations } = useSyncExternalStore(store.subscribe, store.getSnapshot, store.getSnapshot);
  useEffect(() => { store.ensureLoaded(); }, []);

  const categoryIndex = useMemo(() => new Map(categories.map((item) => [item.category_id, item])), [categories]);
  const locationIndex = useMemo(() => new Map(locations.map((item) => [item.id, item])), [locations]);
  const categoryById = useCallback((id) => categoryIndex.get(id), [categoryIndex]);
  const locationById = useCallback((id) => locationIndex.get(id), [locationIndex]);

  return { categories, locations, categoryById, locationById, reload: store.reload, upsertLocation: store.upsertLocation };
}
