package com.bookngo.catalogservice.security;

public record AuthenticatedUser(Long id, String email, String role) {}