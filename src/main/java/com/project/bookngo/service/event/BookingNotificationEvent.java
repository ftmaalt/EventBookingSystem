package com.project.bookngo.service.event;

import java.util.Objects;

public record BookingNotificationEvent(
        Long bookingId,
        BookingNotificationType type
) {
    public BookingNotificationEvent{
        Objects.requireNonNull(bookingId, "bookingId is required");
        Objects.requireNonNull(type, "Notification type is required");
    }
}
