# Google Maps

Set `VITE_GOOGLE_MAPS_API_KEY` in `.env.local`, then restart Vite (or rebuild for deployment). See `.env.example`. Enable Maps JavaScript API and billing in the key's Google Cloud project. Restrict this browser key to your development and production HTTP referrers and Maps JavaScript API. Vite variables are public browser configuration; use a separate key from the backend geocoding key.

The application uses one APIProvider and cached map instances from `@vis.gl/react-google-maps`. Activity details use the existing location lookup; the provider Locations tab previews one saved location at a time. Set the optional `VITE_GOOGLE_MAPS_MAP_ID` to a Maps JavaScript map ID to use AdvancedMarker in the pin editor. Without it, the existing standard marker supports dragging; no extra Google API or backend credential is needed. Read-only previews continue to use standard markers.

Directions use Google's Maps URLs with saved coordinates and Place ID, falling back to address/city when coordinates are unavailable. These links work without a browser API key. Missing, non-finite, or out-of-range coordinates never initialize a map; zero coordinates are valid.

Manual checks:

- Open activity details with a saved location; verify marker, address, directions destination, and session booking links.
- In provider Locations, switch previews and create a location. Verify the original name/address/city request and backend geocoding still work and the saved location appears in the list.
- Remove the browser key and restart Vite; verify the configuration fallback and directions link. Try an invalid key or block Google's script request to check the error fallback.
- Check locations with missing coordinates, an unavailable location, and coordinates of zero. Verify directions still work when an address is available.
- Test narrow screens and keyboard navigation; map controls and directions should remain usable. Check login, provider tabs, booking/waitlist, and SSE notifications as regression smoke tests.

## Provider pin editing

An owning provider can choose **Edit Pin Location**, drag the pin, zoom, or switch to Satellite. Coordinate fields offer a keyboard alternative on all map configurations. AdvancedMarker also supports Google's native keyboard dragging: focus the marker, use Alt/Option + Enter or Space to start moving it, arrow keys to move, Enter/Space to drop, and Escape to cancel the marker movement. The form's Cancel button discards the whole draft.

Only Save sends `PATCH /api/locations/{id}/pin` with numeric `latitude` and `longitude`. Both values are required, finite, and range-validated. The controller requires PROVIDER and the transactional service verifies the current user's provider role and ownership. Existing location endpoints offer no admin ownership override, so the new endpoint does not introduce one. Errors use the existing error response shape with HTTP 400/403/404. No new table or migration is needed.

The same location row is updated, preserving its name, address, city, provider, active flag, and activity references. No Google request runs during a pin update. Its former Place ID is cleared because a manually selected entrance is not a verified match. Directions then use the saved coordinates. A later name-only text edit preserves the pin; explicitly changing the address or city still invokes the existing forward geocoding flow and replaces the geographic data.

Successful writes update the provider preview and all mounted location lookups. BroadcastChannel updates already-open same-origin tabs, including activity maps, when the browser supports it. Other browsers and sessions obtain the saved coordinates on their next API read; this feature does not add or alter server SSE messages. Refreshing a page fetches the authoritative coordinates from PostgreSQL.

## Tests and manual checks

Run `npm test` for pin editor, map adapter, ownership UI, cancellation, duplicate-save protection, error/retry, activity preview synchronization, and remount/read tests. Google Maps itself is mocked in these tests. Run `npm run build` for the production bundle.

Backend: `./mvnw -Dtest=LocationServiceTest,LocationPinUpdateTest,LocationPinPersistenceTest test` (use `mvnw.cmd` on Windows). The persistence test uses the PostgreSQL connection from `application-test.properties`, creates a random `pin_test_<uuid>` schema, checks a committed update in a fresh transaction, then removes only that schema. It does not start application seeders, email, SSE, or web servers. The other location tests mock the repositories and Google service, and exercise HTTP validation/authorization errors through MockMvc.

- With real Google keys, drag to a recognizable entrance in roadmap and satellite modes on desktop and mobile. Verify zoom and map type stay stable while dragging, coordinate fields match the pin, and no PATCH occurs until Save.
- Cancel a moved pin, reopen editing, and confirm the original pin returns with no database change.
- Save a pin, refresh the provider page and activity detail page, and verify both pin coordinates and directions. Check the updated database row has a null Place ID and unchanged descriptive fields and activity references.
- Keep an activity page open in another same-origin tab while saving; its pin should update without reloading when BroadcastChannel is available.
- Rename the location without changing the address/city; verify the manual pin remains. Explicitly change the address/city and verify the normal geocoding flow runs.
- Test a second provider and a USER token: editing UI must be absent for other owners and direct PATCH attempts must return 403. Missing coordinates/out-of-range input must return 400, and an unknown location must return 404 for an authorized provider.
- Verify missing/invalid browser key behavior, keyboard coordinate entry, validation messages, failed-save retry, and booking/waitlist/authentication/notification regression smoke tests.

## Google Maps data-use review

Reviewed the current [Maps Platform terms](https://cloud.google.com/maps-platform/terms), [service-specific terms](https://cloud.google.com/maps-platform/terms/maps-service-terms), and [Geocoding policies](https://developers.google.com/maps/documentation/geocoding/policies). Keep Google's built-in attribution visible. The implementation stores the provider's explicit selected coordinates; it does not cache tiles, imagery, Google place details, or reverse-geocoding responses, and it does not claim that an entrance is a Google-verified venue.

Manual input should not be treated as a blanket exemption from Google's restrictions on deriving content from Google Maps. The terms restrict scraping, caching Google Maps Content, and tracing/digitizing map features. The existing Google-geocoded coordinates also need a separate retention review: current non-EEA terms allow general geocode caching for 30 days and a narrower indefinite exception that requires isolation to a specific end user, without cross-user reuse. A shared booking platform should not assume that exception covers all its location data. Place IDs have their own storage exception. This feature neither automatically expires manually selected venue pins nor periodically re-geocodes them; origin/retention tracking for existing geocoded data is a separate follow-up. Confirm the terms applicable to the project's billing region and agreement before relying on long-term storage of Google-derived data.

Next iteration: Places Autocomplete needs Places API enablement and key restrictions, a Places library integration, and an agreed Place ID/address request contract. It remains out of scope. No reverse geocoding is used to infer or certify venue identity.

## Files changed for draggable pins

- Frontend: `src/components/LocationMap.jsx`, new `src/components/LocationPinEditor.jsx`, `src/pages/Provider.jsx`, `src/api.js`, `src/utils/useLookups.js`, and `src/styles.css`.
- Setup/documentation: `.env.example`, `MAPS.md`, `package.json`, and `package-lock.json` (Vitest, jsdom, and React Testing Library are development dependencies).
- Frontend tests: `src/components/LocationMap.test.jsx`, `src/components/LocationPinEditor.test.jsx`, `src/pages/ProviderLocations.test.jsx`, and `src/utils/useLookups.test.jsx`.
- Backend under `src/main/java/com/project/bookngo/`: new `model/request/LocationPinUpdateRequest.java`, `controller/LocationController.java`, `service/LocationService.java`, and `exception/GlobalExceptionHandler.java`. The existing Location entity and response need no further change for pins.
- Backend tests under `src/test/java/com/project/bookngo/service/`: `LocationServiceTest.java`, new `LocationPinUpdateTest.java`, and new `LocationPinPersistenceTest.java`.

References: [vis.gl APIProvider](https://visgl.github.io/react-google-maps/docs/api-reference/components/api-provider), [map instance caching](https://visgl.github.io/react-google-maps/docs/api-reference/components/map), [Google Maps URLs](https://developers.google.com/maps/documentation/urls/guide).
