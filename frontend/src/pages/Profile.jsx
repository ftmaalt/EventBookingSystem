import { useEffect, useState } from "react";

import { authApi, usersApi } from "../api";
import { useAuth } from "../auth";
import Layout from "../components/Layout";
import { Feedback, Input, SectionHeader, StatusPill } from "../components/ui";
import { fileUrl, getError } from "../utils/format";

const MAX_PICTURE_BYTES = 5 * 1024 * 1024;
// Must match what the backend accepts (FileStorageService).
const ALLOWED_PICTURE_TYPES = ["image/jpeg", "image/png", "image/webp"];

export default function Profile() {
  const { user, refreshUser } = useAuth();

  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [pictureVersion, setPictureVersion] = useState(Date.now());

  useEffect(() => {
    if (user) {
      setName(user.fullName || "");
      setPhone(user.phone || "");
    }
  }, [user]);

  const displayName = user?.fullName || "User";
  const picture = user?.profilePicturePath ? `${fileUrl(user.profilePicturePath)}?v=${pictureVersion}` : "";
  const initials =
    displayName
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join("") || "U";

  const reset = () => {
    setMessage("");
    setError("");
  };

  const updateProfile = async (event) => {
    event.preventDefault();
    reset();

    if (!name.trim()) {
      setError("Full name is required.");
      return;
    }
    if (phone && !/^\d{8}$/.test(phone)) {
      setError("Phone number must be exactly 8 digits.");
      return;
    }

    try {
      await usersApi.updateMe({ fullName: name.trim(), phone });
      await refreshUser();
      setMessage("Profile updated successfully.");
    } catch (err) {
      setError(getError(err));
    }
  };

  const uploadPicture = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) {
      return;
    }
    reset();

    if (!ALLOWED_PICTURE_TYPES.includes(file.type)) {
      setError("Please select a JPG, PNG or WEBP image.");
      return;
    }
    if (file.size > MAX_PICTURE_BYTES) {
      setError("Profile picture must be smaller than 5 MB.");
      return;
    }

    try {
      await usersApi.uploadProfilePicture(file);
      await refreshUser();
      setPictureVersion(Date.now());
      setMessage("Profile picture uploaded successfully.");
    } catch (err) {
      setError(getError(err));
    }
  };

  const changePassword = async (event) => {
    event.preventDefault();
    reset();

    if (!currentPassword) {
      setError("Enter your current password.");
      return;
    }
    if (newPassword.length < 8) {
      setError("New password must contain at least 8 characters.");
      return;
    }

    try {
      await authApi.changePassword({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      setMessage("Password changed successfully.");
    } catch (err) {
      setError(getError(err));
    }
  };

  return (
    <Layout>
      <div className="page">
        <SectionHeader title="Profile & Settings" text="Manage your account." />

        <div className="profile-grid">
          <div className="profile-card">
            <div className="avatar">{picture ? <img src={picture} alt="Profile" /> : <span>{initials}</span>}</div>
            <h2>{displayName}</h2>
            <p>{user?.email}</p>
            {user?.role && <StatusPill status={user.role} />}

            <label className="upload-box">
              {picture ? "Change Profile Picture" : "Upload Profile Picture"}
              <input type="file" accept={ALLOWED_PICTURE_TYPES.join(",")} onChange={uploadPicture} />
            </label>
            <small className="profile-picture-help">JPG, PNG or WEBP · max 5 MB</small>
          </div>

          <div className="profile-forms">
            <Feedback error={error} message={message} />

            <form className="form-card" onSubmit={updateProfile}>
              <h2>Personal Information</h2>
              <Input label="Full name" value={name} onChange={setName} />
              <Input
                label="Phone"
                type="tel"
                inputMode="numeric"
                maxLength={8}
                value={phone}
                onChange={(value) => setPhone(value.replace(/\D/g, ""))}
                hint="8 digits"
              />
              <button className="primary-button">Save Changes</button>
            </form>

            <form className="form-card" onSubmit={changePassword}>
              <h2>Change Password</h2>
              <Input
                label="Current password"
                type="password"
                value={currentPassword}
                onChange={setCurrentPassword}
                autoComplete="current-password"
              />
              <Input
                label="New password"
                type="password"
                value={newPassword}
                onChange={setNewPassword}
                autoComplete="new-password"
              />
              <button className="secondary-button">Change Password</button>
            </form>
          </div>
        </div>
      </div>
    </Layout>
  );
}
