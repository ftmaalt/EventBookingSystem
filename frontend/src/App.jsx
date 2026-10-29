import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";

import Protected from "./components/Protected";
import Layout from "./components/Layout";
import { Loading } from "./components/ui";
import Landing from "./pages/Landing";
import NotFound from "./pages/NotFound";

// Keep the landing page immediate; load other pages only when their route opens.
const Activities = lazy(() => import("./pages/Activities"));
const ActivityDetails = lazy(() => import("./pages/ActivityDetails"));
const Admin = lazy(() => import("./pages/Admin"));
const ApplyProvider = lazy(() => import("./pages/ApplyProvider"));
const Booking = lazy(() => import("./pages/Booking"));
const Bookings = lazy(() => import("./pages/Bookings"));
const Dashboard = lazy(() => import("./pages/Dashboard"));
const ForgotPassword = lazy(() => import("./pages/ForgotPassword"));
const Login = lazy(() => import("./pages/Login"));
const Profile = lazy(() => import("./pages/Profile"));
const Provider = lazy(() => import("./pages/Provider"));
const Register = lazy(() => import("./pages/Register"));
const ResetPassword = lazy(() => import("./pages/ResetPassword"));
const VerifyEmail = lazy(() => import("./pages/VerifyEmail"));

/** Wraps a page so only logged-in users (optionally with one of `roles`) can open it. */
function guard(page, roles) {
  return <Protected roles={roles}>{page}</Protected>;
}

export default function App() {
  return (
    <Suspense fallback={<Layout><Loading /></Layout>}>
      <Routes>
        {/* Public */}
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-email" element={<VerifyEmail />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/activities" element={<Activities />} />
        <Route path="/activities/:id" element={<ActivityDetails />} />

        {/* Any logged-in user */}
        <Route path="/dashboard" element={guard(<Dashboard />)} />
        <Route path="/book/:id" element={guard(<Booking />)} />
        <Route path="/bookings" element={guard(<Bookings />)} />
        <Route path="/profile" element={guard(<Profile />)} />

        {/* Role specific */}
        <Route path="/apply-provider" element={guard(<ApplyProvider />, ["USER"])} />
        <Route path="/provider" element={guard(<Provider />, ["PROVIDER"])} />
        <Route path="/admin" element={guard(<Admin />, ["ADMIN"])} />

        <Route path="*" element={<NotFound />} />
      </Routes>
    </Suspense>
  );
}
