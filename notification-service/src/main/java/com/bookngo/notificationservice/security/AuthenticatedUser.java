package com.bookngo.notificationservice.security;

/** The logged-in user as described by the JWT. No database lookup involved. */
public record AuthenticatedUser(Long id, String email, String role) {}
