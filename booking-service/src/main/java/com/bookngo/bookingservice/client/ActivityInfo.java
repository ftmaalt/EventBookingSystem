package com.bookngo.bookingservice.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/** The few activity fields booking-service needs from catalog-service. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ActivityInfo(
        @JsonProperty("activity_id") Long activityId,
        String title,
        BigDecimal pricePerPerson,
        Long providerId,
        String status
) {}
