package com.bookngo.notificationservice.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;


@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long bookingId,
        Long userId,
        String userEmail,
        Long sessionId,
        Long activityId,
        String activityTitle,
        LocalDateTime startTime,
        Integer participants,
        String bookingType,
        BigDecimal totalPrice
) {}
