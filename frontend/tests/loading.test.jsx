import { afterEach, beforeEach, expect, it, vi } from "vitest";
import { cleanup, render, screen } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import App from "../src/App";

const state = vi.hoisted(() => ({ token: null, role: null, loading: false, providerImport: vi.fn() }));
vi.mock("../src/auth", () => ({ useAuth: () => state }));
vi.mock("../src/components/Layout", () => ({ default: ({ children }) => <>{children}</> }));
vi.mock("../src/pages/Landing", () => ({ default: () => <h1>Home</h1> }));
vi.mock("../src/pages/Provider", () => {
  state.providerImport();
  return { default: () => <h1>Provider page</h1> };
});
vi.mock("../src/pages/Dashboard", () => ({ default: () => <h1>Dashboard page</h1> }));
vi.mock("../src/pages/Login", () => ({ default: () => {
  const location = useLocation();
  return <h1>Login from {location.state?.from}</h1>;
} }));

beforeEach(() => { state.token = null; state.role = null; state.loading = false; state.providerImport.mockClear(); });
afterEach(cleanup);

it("renders the landing page without loading the provider page", () => {
  render(<MemoryRouter><App /></MemoryRouter>);
  expect(screen.getByRole("heading", { name: "Home" })).toBeTruthy();
  expect(state.providerImport).not.toHaveBeenCalled();
});

it("redirects a guest before loading the protected page and remembers the full destination", async () => {
  render(<MemoryRouter initialEntries={["/provider?tab=locations"]}><App /></MemoryRouter>);
  await screen.findByRole("heading", { name: "Login from /provider?tab=locations" });
  expect(state.providerImport).not.toHaveBeenCalled();
});

it("keeps provider authorization enforced for a USER", async () => {
  state.token = "test-token";
  state.role = "USER";
  render(<MemoryRouter initialEntries={["/provider"]}><App /></MemoryRouter>);
  await screen.findByRole("heading", { name: "Dashboard page" });
  expect(screen.queryByRole("heading", { name: "Provider page" })).toBeNull();
});

it("loads the provider page for an authorized provider", async () => {
  state.token = "test-token";
  state.role = "PROVIDER";
  render(<MemoryRouter initialEntries={["/provider"]}><App /></MemoryRouter>);
  await screen.findByRole("heading", { name: "Provider page" });
});
