import { useEffect, useState } from "react";
import { AdvancedMarker, APIProvider, APILoadingStatus, Map, Marker, useApiLoadingStatus, useMap } from "@vis.gl/react-google-maps";

const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY?.trim();
const mapId = import.meta.env.VITE_GOOGLE_MAPS_MAP_ID?.trim();
const libraries = mapId ? ["marker"] : [];

function FollowPin({ position }) {
  const map = useMap();
  useEffect(() => {
    map?.panTo(position);
  }, [map, position.lat, position.lng]);
  return null;
}

export function getCoordinates(location) {
  const lat = location?.latitude;
  const lng = location?.longitude;
  return typeof lat === "number" && typeof lng === "number" &&
    Number.isFinite(lat) && Number.isFinite(lng) && Math.abs(lat) <= 90 && Math.abs(lng) <= 180
    ? { lat, lng } : null;
}

export function getDirectionsUrl(location) {
  if (!location) return null;
  const position = getCoordinates(location);
  const address = [location.address, location.city].filter(Boolean).join(", ");
  const destination = position ? `${position.lat},${position.lng}` : address || location.name;
  if (!destination) return null;
  const params = new URLSearchParams({ api: "1", destination });
  if (location.googlePlaceId) params.set("destination_place_id", location.googlePlaceId);
  return `https://www.google.com/maps/dir/?${params}`;
}

function MapPreview({ position, name, editable, onPositionChange, disabled }) {
  const status = useApiLoadingStatus();
  const [timedOut, setTimedOut] = useState(false);
  useEffect(() => {
    // Blocked scripts can remain pending instead of reporting a loader error.
    if (status === APILoadingStatus.LOADED) return;
    const timer = window.setTimeout(() => setTimedOut(true), 20000);
    return () => window.clearTimeout(timer);
  }, [status]);
  if (status === APILoadingStatus.FAILED || status === APILoadingStatus.AUTH_FAILURE ||
    (timedOut && status !== APILoadingStatus.LOADED)) {
    return <p className="location-map-state" role="status">Google Maps is unavailable. Use the directions link below.</p>;
  }
  if (status !== APILoadingStatus.LOADED) {
    return <p className="location-map-state" role="status">Loading Google Maps…</p>;
  }
  const markerProps = {
    position,
    title: editable ? `Move pin for ${name}` : name,
    draggable: editable && !disabled,
    onDragEnd: editable ? (event) => {
      if (!disabled && event.latLng) onPositionChange(event.latLng.toJSON());
    } : undefined,
  };
  return (
    <Map key={editable ? "editing" : `${position.lat},${position.lng}`} defaultCenter={position} defaultZoom={editable ? 18 : 15}
      mapId={editable ? mapId : undefined}
      reuseMaps gestureHandling="cooperative" streetViewControl={false} mapTypeControl={Boolean(editable)} zoomControl
      style={{ width: "100%", height: "100%" }} aria-label={`Map showing ${name}`}>
      {/* Advanced markers require a map ID; existing installations retain a draggable standard marker. */}
      {editable && mapId ? <AdvancedMarker {...markerProps} /> : <Marker {...markerProps} />}
      {editable && <FollowPin position={position} />}
    </Map>
  );
}

export default function LocationMap({ location, editable = false, onPositionChange, disabled = false }) {
  const position = getCoordinates(location);
  const url = getDirectionsUrl(location);

  return (
    <section className="location-map" aria-label={`Location map: ${location?.name || "Location unavailable"}`}>
      <div className="location-map-canvas">
        {!position ? (
          <p className="location-map-state" role="status">Map coordinates are unavailable for this location.</p>
        ) : !apiKey ? (
          <p className="location-map-state" role="status">Map preview is not configured. Use the directions link below.</p>
        ) : <APIProvider apiKey={apiKey} libraries={libraries}>
          {/* The library shares its script loader and map cache across mounts. */}
          <MapPreview position={position} name={location?.name || "Activity location"}
            editable={editable} onPositionChange={onPositionChange} disabled={disabled} />
        </APIProvider>}
      </div>
      {url && <a className="location-map-link" href={url} target="_blank" rel="noopener noreferrer">Open in Google Maps <span className="muted">(opens in a new tab)</span></a>}
    </section>
  );
}
