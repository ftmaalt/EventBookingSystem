package com.bookngo.bookingservice.event;

import com.bookngo.bookingservice.model.Bookings;
import com.bookngo.bookingservice.model.Sessions;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * A booking domain event, published to the Kafka topic "booking-events".
 * a snapshot that carries everything a consumer needs, so no consumer ever has to call
 * booking-service. eventId is unique per event and lets consumers detect a redelivered message.
 */
public record BookingEvent(
        String eventId,
        BookingEventType eventType,
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
) {
    public BookingEvent {
        Objects.requireNonNull(eventId, "eventId is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(bookingId, "bookingId is required");
        Objects.requireNonNull(userId, "userId is required");
        Objects.requireNonNull(sessionId, "sessionId is required");
    }

    public static BookingEvent of(Bookings booking, BookingEventType type) {
        Sessions session = booking.getSession();
        return new BookingEvent(
                UUID.randomUUID().toString(),
                type,
                Instant.now(),
                booking.getBookingId(),
                booking.getUserId(),
                booking.getUserEmail(),
                session.getId(),
                session.getActivityId(),
                session.getActivityTitle(),
                session.getStartTime(),
                booking.getParticipants(),
                booking.getBookingType() == null ? null : booking.getBookingType().name(),
                booking.getTotalPrice());
    }
}
