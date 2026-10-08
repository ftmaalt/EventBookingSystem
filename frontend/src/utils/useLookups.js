import { useCallback, useEffect, useState } from "react";

import { categoriesApi, locationsApi } from "../api";

/** Loads categories and locations (public reference data) and offers lookups by id. */
export function useLookups() {
  const [categories, setCategories] = useState([]);
  const [locations, setLocations] = useState([]);

  const reload = useCallback(async () => {
    const [categoryResult, locationResult] = await Promise.allSettled([categoriesApi.getAll(), locationsApi.getAll()]);
    if (categoryResult.status === "fulfilled") {
      setCategories(categoryResult.value.data);
    }
    if (locationResult.status === "fulfilled") {
      setLocations(locationResult.value.data);
    }
  }, []);

  useEffect(() => {
    reload();
  }, [reload]);

  const categoryById = (id) => categories.find((item) => item.category_id === id);
  const locationById = (id) => locations.find((item) => item.id === id);

  return { categories, locations, categoryById, locationById, reload };
}
