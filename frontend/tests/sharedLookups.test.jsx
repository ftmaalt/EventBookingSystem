import { StrictMode } from "react";
import { afterEach, expect, it, vi } from "vitest";
import { act, cleanup, renderHook, waitFor } from "@testing-library/react";
import { categoriesApi, locationsApi } from "../src/api";
import { useLookups } from "../src/utils/useLookups";

vi.mock("../src/api", () => ({
  categoriesApi: { getAll: vi.fn(async () => ({ data: [{ category_id: 3, category_name: "Arts" }] })) },
  locationsApi: { getAll: vi.fn(async () => ({ data: [{ id: 10, latitude: 26.2, longitude: 50.5 }] })) },
}));
afterEach(cleanup);

it("deduplicates StrictMode consumers and propagates a saved pin through both hooks", async () => {
  const { result } = renderHook(() => [useLookups(), useLookups()], {
    wrapper: ({ children }) => <StrictMode>{children}</StrictMode>,
  });
  await waitFor(() => expect(result.current[0].locationById(10)?.latitude).toBe(26.2));
  expect(categoriesApi.getAll).toHaveBeenCalledOnce();
  expect(locationsApi.getAll).toHaveBeenCalledOnce();
  act(() => result.current[0].upsertLocation({ id: 10, latitude: 26.3, longitude: 50.6, googlePlaceId: null }));
  expect(result.current[1].locationById(10).latitude).toBe(26.3);
  expect(result.current[0].categoryById(3).category_name).toBe("Arts");
});
