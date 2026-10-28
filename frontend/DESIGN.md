# The Social Edit — Plum & Sage

The redesign stays within React/Vite, React Router, and the existing CSS architecture. No new dependencies, backend contracts, authentication behavior, booking rules, notification/SSE behavior, or map/pin editing logic were introduced or changed. The existing star image placeholders are retained; no photographs or new image assets are included.

## Design and shared components

- `styles.css` centralizes the requested plum, lilac, sage, ivory, sand, and charcoal colors. Accessible darker semantic text colors accompany the softer accents. The established Google Fonts import continues to use Playfair Display and DM Sans, with system fallbacks.
- `theme.css`, imported after the existing CSS, applies the identity to public pages, authentication, reservations, profiles, and user/provider/admin dashboards. Existing layout classes remain available. Tables retain horizontal scrolling, while cards/forms adapt to narrow screens.
- `Layout.jsx` provides the wordmark, active navigation, search, authenticated profile menu, role-specific links, notifications, responsive menu, skip link, and footer. Mobile navigation supports Escape and outside-click dismissal.
- `ActivityCard.jsx` provides the placeholder, real category/location data, title, description, duration, price, and detail navigation on the discovery page.
- New `SearchBar.jsx` routes homepage/header searches to the existing discovery page using an encoded `q` parameter.
- `ui.jsx` adds reusable activity loading skeletons and optional section labels/actions. Loading, validation, and success messages have accessible status semantics.

Focus outlines, labeled search/filter inputs, touch-sized controls, color contrast, and `prefers-reduced-motion` styles are shared throughout the frontend. Native details/summary provides the account dropdown without a new dependency.

## Pages and real-data boundaries

The homepage retains search, real category navigation, the how-it-works section, and the account call to action. The Social Edit and More to explore sections and their activity-fetching code were removed at the user's request. Category navigation uses the current category lookup and links to `?category=<id>`. Experience browsing remains on the discovery page.

The hero displays the user-supplied Eleanor Roosevelt quote in the original arched plum card, with ivory text, a fine inset border, and lilac shadow. The category section also has a plum background with light category cards. This is CSS and text only. The top announcement strip, wordmark subtitle, hero label/footnote/floating note, and footer's Social Edit tagline were removed from JSX, with their unused styling removed as well.

Activity discovery keeps its existing pagination, category/location filtering, 300 ms search debounce, and client-side text matching. It additionally accepts header/homepage `q` and `category` parameters, including navigation searches while the page is already mounted. The existing text-search limit of 100 fetched activities remains unchanged.

Activity list responses contain no session dates or remaining capacity. Cards direct visitors to the real session list rather than making per-card session requests or inventing availability. Activity details continue to fetch the existing activity and session endpoints, display real dates/capacity, apply the unchanged `isBookable` check, and preserve booking/login destinations. The new booking summary uses the first genuinely bookable session. Saved maps and Google directions remain in the detail page.

Authentication forms, provider location creation/editing/pin tools, and dashboard operations retain their existing handlers. Their appearance changes through the shared theme. No backend files were edited for this redesign.

## Files

Modified:

- `frontend/index.html`
- `frontend/src/main.jsx`
- `frontend/src/styles.css`
- `frontend/src/components/Layout.jsx`
- `frontend/src/components/ui.jsx`
- `frontend/src/pages/Landing.jsx`
- `frontend/src/pages/Activities.jsx`
- `frontend/src/pages/ActivityDetails.jsx`

Added:

- `frontend/src/theme.css`
- `frontend/src/components/ActivityCard.jsx`
- `frontend/src/components/SearchBar.jsx`
- `frontend/src/components/Layout.test.jsx`
- `frontend/src/pages/Discovery.test.jsx`
- `frontend/DESIGN.md`

## Verification

`npm test`: 22 tests passed, including the existing map/pin suites and new navigation/search/role/real-data tests. `npm run build`: passed. No dependency changes were required.

Headless Edge screenshots were reviewed for public homepage, discovery, details, login/registration, provider Locations, user dashboard, and admin pages at desktop/tablet/mobile widths. Tested widths included 390, 768, and 1440 pixels, with no horizontal page overflow. Mobile menu opening and Escape dismissal passed. Populated pages used isolated browser-only API fixtures because the backend was unreachable during review; fixtures are not in application code. Actual empty/API-error views were checked separately. Google Maps remained in its loading/fallback state in this environment, so live mapping and authenticated server operations still need a normal connected-browser smoke test.

Manual checks with the backend running:

1. Explore categories, search from header/homepage, change filters, paginate, and navigate back/forward.
2. Open an experience, verify map/directions and real session dates/spots, then complete a booking. Check full/past/unavailable sessions and existing waitlist behavior.
3. Log in as USER, PROVIDER, and ADMIN. Check role navigation, profile/logout, dashboard tabs, tables, and existing forms.
4. Create/edit a location, drag and cancel/save its pin, and verify the customer map still reflects the saved coordinates.
5. Check login/register/reset/verification validation, profile uploads, booking cancellation, and SSE notifications/toasts.
6. Use keyboard navigation, a narrow phone screen, browser zoom, and reduced-motion preferences.
