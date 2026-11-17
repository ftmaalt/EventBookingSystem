package com.bookngo.complianceservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CancelledSessionsInfo(int sessionsCancelled, int bookingsCancelled) {}
