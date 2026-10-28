import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import Layout from "./Layout";

const auth = vi.hoisted(() => ({ token: null, role: null, user: null, logout: vi.fn() }));
vi.mock("../auth", () => ({ useAuth: () => auth }));
vi.mock("./NotificationCenter", () => ({ default: () => <button>Notifications</button> }));
vi.mock("./NotificationToasts", () => ({ default: () => null }));
function Location() { const location = useLocation(); return <output>{location.pathname}{location.search}</output>; }
function open(initial = "/") { return render(<MemoryRouter initialEntries={[initial]}><Layout><h1>Page</h1><Location /></Layout></MemoryRouter>); }
beforeEach(() => { auth.token = null; auth.role = null; auth.user = null; auth.logout.mockClear(); });
afterEach(cleanup);

it("supports navigation search and a mobile menu that closes with Escape", () => {
  open();
  const button = screen.getByRole("button", { name: "Open menu" });
  fireEvent.click(button);
  expect(button.getAttribute("aria-expanded")).toBe("true");
  fireEvent.keyDown(document, { key: "Escape" });
  expect(button.getAttribute("aria-expanded")).toBe("false");
  expect(document.activeElement).toBe(button);
  fireEvent.change(screen.getByRole("searchbox"), { target: { value: "music" } });
  fireEvent.submit(screen.getByRole("search"));
  expect(screen.getByText("/activities?q=music")).toBeTruthy();
});

it.each(["USER", "PROVIDER", "ADMIN"])("preserves %s role navigation, profile access and logout", (role) => {
  auth.token = "test-token"; auth.role = role; auth.user = { fullName: "Test Person" };
  open("/dashboard");
  const nav = within(screen.getByRole("navigation", { name: "Main navigation" }));
  const label = role === "USER" ? "Become a Provider" : role === "PROVIDER" ? "Provider" : "Admin";
  expect(nav.getByRole("link", { name: label })).toBeTruthy();
  if (role !== "ADMIN") expect(nav.queryByRole("link", { name: "Admin" })).toBeNull();
  fireEvent.click(screen.getByText("My account"));
  expect(nav.getByRole("link", { name: "Profile" }).getAttribute("href")).toBe("/profile");
  fireEvent.click(screen.getByRole("button", { name: "Logout" }));
  expect(auth.logout).toHaveBeenCalledOnce();
  expect(screen.getByText("/", { selector: "output" })).toBeTruthy();
});
