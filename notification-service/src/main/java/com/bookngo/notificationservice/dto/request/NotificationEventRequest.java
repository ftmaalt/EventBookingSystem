package com.bookngo.notificationservice.dto.request;

import com.bookngo.notificationservice.model.enums.NotificationType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * "Something happened, tell the user." One flat shape for all types; each type uses only the fields it needs.
 * Other services (or, later, a Kafka consumer) send this; they never build the message text themselves.
 */
public record NotificationEventRequest(
        @NotNull NotificationType type,
        Long userId,            // recipient
        String email,           // recipient e-mail
        Long bookingId,
        String activityTitle,
        String startTime,
        Integer participants,
        String bookingType,
        BigDecimal totalPrice,
        String reason,
        String token,
        String businessName,
        String contactName,
        String city,
        String proposedActivities
) {}
