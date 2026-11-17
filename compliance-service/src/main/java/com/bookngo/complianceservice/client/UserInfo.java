package com.bookngo.complianceservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserInfo(Long id, String email, String role, String status) {}
