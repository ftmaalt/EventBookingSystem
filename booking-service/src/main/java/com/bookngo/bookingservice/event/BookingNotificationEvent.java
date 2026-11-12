package com.bookngo.bookingservice.event;

import java.util.Objects;

/** Carries everything a consumer needs, so it never has to query booking's tables. */
public record BookingNotificationEvent(
        Long bookingId,
        Long userId,
        String userEmail,
        Long sessionId,
        BookingNotificationType type
) {
    public BookingNotificationEvent {
        Objects.requireNonNull(bookingId, "bookingId is required");
        Objects.requireNonNull(type, "Notification type is required");
    }
}
