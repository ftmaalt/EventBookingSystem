package com.bookngo.notificationservice.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;


@JsonIgnoreProperties(ignoreUnknown = true)
public record AccountEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        Long recipientUserId,
        String recipientEmail,
        String token,
        String businessName,
        String contactName,
        String city,
        String proposedActivities,
        String reviewNote
) {}
