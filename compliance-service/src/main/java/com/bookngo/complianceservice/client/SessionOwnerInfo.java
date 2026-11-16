package com.bookngo.complianceservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public record SessionOwnerInfo(Long id, Long providerId, String status) {}
