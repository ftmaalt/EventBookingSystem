import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { locationsApi } from "../api";
import LocationPinEditor from "./LocationPinEditor";

vi.mock("../api", () => ({ locationsApi: { updatePin: vi.fn() } }));
vi.mock("./LocationMap", async (importOriginal) => ({
  ...await importOriginal(),
  default: ({ location, onPositionChange, disabled }) => <div data-testid="map"
    data-latitude={location.latitude} data-longitude={location.longitude}>
    <button type="button" disabled={disabled} onClick={() => onPositionChange({ lat: 26.24, lng: 50.58 })}>Drag pin</button>
  </div>,
}));

const location = { id: 10, name: "Center", latitude: 26.2, longitude: 50.5, googlePlaceId: "old-place" };

describe("LocationPinEditor", () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(cleanup);

  it("keeps dragging temporary and Cancel makes no save request", () => {
    const onCancel = vi.fn();
    render(<LocationPinEditor location={location} onSaved={vi.fn()} onCancel={onCancel} />);
    fireEvent.click(screen.getByText("Drag pin"));
    expect(screen.getByLabelText("Latitude").value).toBe("26.24");
    expect(screen.getByTestId("map").dataset.latitude).toBe("26.24");
    fireEvent.click(screen.getByText("Cancel"));
    expect(onCancel).toHaveBeenCalledOnce();
    expect(locationsApi.updatePin).not.toHaveBeenCalled();
    expect(location.latitude).toBe(26.2);
  });

  it("sends only the final coordinates on Save and returns the authoritative response", async () => {
    const saved = { ...location, latitude: 26.24, longitude: 50.58, googlePlaceId: null };
    locationsApi.updatePin.mockResolvedValue({ data: saved });
    const onSaved = vi.fn();
    render(<LocationPinEditor location={location} onSaved={onSaved} onCancel={vi.fn()} />);
    fireEvent.click(screen.getByText("Drag pin"));
    expect(locationsApi.updatePin).not.toHaveBeenCalled();
    fireEvent.click(screen.getByText("Save Pin Location"));
    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
    expect(locationsApi.updatePin).toHaveBeenCalledExactlyOnceWith(10, { latitude: 26.24, longitude: 50.58 });
  });

  it("prevents duplicate saves while a request is processing", async () => {
    let finish;
    locationsApi.updatePin.mockReturnValue(new Promise((resolve) => { finish = resolve; }));
    const onSaved = vi.fn();
    render(<LocationPinEditor location={location} onSaved={onSaved} onCancel={vi.fn()} />);
    fireEvent.click(screen.getByText("Drag pin"));
    const form = screen.getByRole("form");
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(locationsApi.updatePin).toHaveBeenCalledOnce();
    expect(screen.getByText("Saving…").disabled).toBe(true);
    expect(screen.getByText("Cancel").disabled).toBe(true);
    finish({ data: location });
    await waitFor(() => expect(onSaved).toHaveBeenCalledOnce());
  });

  it("supports keyboard coordinate input and rejects an empty or out-of-range draft", () => {
    render(<LocationPinEditor location={location} onSaved={vi.fn()} onCancel={vi.fn()} />);
    fireEvent.change(screen.getByLabelText("Latitude"), { target: { value: "91" } });
    fireEvent.submit(screen.getByRole("form"));
    expect(screen.getByText(/Enter a latitude between/)).toBeTruthy();
    expect(locationsApi.updatePin).not.toHaveBeenCalled();
    fireEvent.change(screen.getByLabelText("Latitude"), { target: { value: "" } });
    expect(screen.getByText("Save Pin Location").disabled).toBe(true);
    fireEvent.change(screen.getByLabelText("Latitude"), { target: { value: "0" } });
    fireEvent.change(screen.getByLabelText("Longitude"), { target: { value: "0" } });
    expect(screen.getByTestId("map").dataset.latitude).toBe("0");
    expect(screen.getByText("Save Pin Location").disabled).toBe(false);
  });

  it("keeps the draft available for retry when the API rejects the save", async () => {
    locationsApi.updatePin.mockRejectedValue({ userMessage: "You are not authorized to do that." });
    const onSaved = vi.fn();
    render(<LocationPinEditor location={location} onSaved={onSaved} onCancel={vi.fn()} />);
    fireEvent.click(screen.getByText("Drag pin"));
    fireEvent.click(screen.getByText("Save Pin Location"));
    await screen.findByText("You are not authorized to do that.");
    expect(screen.getByLabelText("Latitude").value).toBe("26.24");
    expect(screen.getByText("Save Pin Location").disabled).toBe(false);
    expect(onSaved).not.toHaveBeenCalled();
  });
});
