import { Route, Routes } from "react-router-dom";

import Protected from "./components/Protected";
import Activities from "./pages/Activities";
import ActivityDetails from "./pages/ActivityDetails";
import Admin from "./pages/Admin";
import ApplyProvider from "./pages/ApplyProvider";
import Booking from "./pages/Booking";
import Bookings from "./pages/Bookings";
import Dashboard from "./pages/Dashboard";
import ForgotPassword from "./pages/ForgotPassword";
import Landing from "./pages/Landing";
import Login from "./pages/Login";
import NotFound from "./pages/NotFound";
import Profile from "./pages/Profile";
import Provider from "./pages/Provider";
import Register from "./pages/Register";
import ResetPassword from "./pages/ResetPassword";
import VerifyEmail from "./pages/VerifyEmail";

/** Wraps a page so only logged-in users (optionally with one of `roles`) can open it. */
function guard(page, roles) {
  return <Protected roles={roles}>{page}</Protected>;
}

export default function App() {
  return (
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
  );
}
