import { useRef, useState } from "react";

import { locationsApi } from "../api";
import { getError } from "../utils/format";
import LocationMap, { getCoordinates } from "./LocationMap";
import { Feedback, Input } from "./ui";

export default function LocationPinEditor({ location, onSaved, onCancel }) {
  const [draft, setDraft] = useState({ latitude: String(location.latitude), longitude: String(location.longitude) });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const submitting = useRef(false);
  const position = draft.latitude.trim() && draft.longitude.trim()
    ? getCoordinates({ latitude: Number(draft.latitude), longitude: Number(draft.longitude) }) : null;
  const unchanged = position?.lat === location.latitude && position?.lng === location.longitude;

  const save = async (event) => {
    event.preventDefault();
    if (submitting.current) return;
    if (!position) {
      setError("Enter a latitude between -90 and 90 and a longitude between -180 and 180.");
      return;
    }
    if (unchanged) return;
    submitting.current = true;
    setSaving(true);
    setError("");
    try {
      const response = await locationsApi.updatePin(location.id, { latitude: position.lat, longitude: position.lng });
      onSaved(response.data);
    } catch (err) {
      setError(getError(err));
    } finally {
      submitting.current = false;
      setSaving(false);
    }
  };

  return (
    <form onSubmit={save} aria-label={`Edit pin for ${location.name}`} aria-busy={saving}>
      <h3>Edit pin: {location.name}</h3>
      <p className="muted">Drag the pin to the building or entrance. Zoom in or choose Satellite for more detail.
        You can also enter coordinates with the keyboard. Changes are temporary until saved.</p>
      <LocationMap location={{ ...location, latitude: position?.lat ?? location.latitude,
        longitude: position?.lng ?? location.longitude, googlePlaceId: null }} editable disabled={saving}
        onPositionChange={({ lat, lng }) => setDraft({ latitude: String(lat), longitude: String(lng) })} />
      <div className="pin-coordinate-fields">
        <Input label="Latitude" type="number" min={-90} max={90} step="any" required disabled={saving}
          value={draft.latitude} onChange={(latitude) => setDraft((previous) => ({ ...previous, latitude }))} />
        <Input label="Longitude" type="number" min={-180} max={180} step="any" required disabled={saving}
          value={draft.longitude} onChange={(longitude) => setDraft((previous) => ({ ...previous, longitude }))} />
      </div>
      <p role="status">{position ? `Unsaved position: ${position.lat.toFixed(7)}, ${position.lng.toFixed(7)}`
        : "Enter valid coordinates to preview the new position."}</p>
      <div aria-live="polite"><Feedback error={error} /></div>
      <div className="button-row">
        <button className="primary-button" disabled={saving || !position || unchanged}>
          {saving ? "Saving…" : "Save Pin Location"}
        </button>
        <button type="button" className="secondary-button" disabled={saving} onClick={onCancel}>Cancel</button>
      </div>
    </form>
  );
}
