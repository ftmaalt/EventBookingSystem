package com.bookngo.complianceservice.security;

public record AuthenticatedUser(Long id, String email, String role) {}
