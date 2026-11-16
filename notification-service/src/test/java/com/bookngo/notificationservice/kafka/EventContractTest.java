package com.bookngo.notificationservice.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the JSON contract between booking-service (producer) and this consumer, using the same
 * serializer settings as application.properties: no type headers, consumer chooses its own class.
 */
class EventContractTest {

    /** Same shape as booking-service's BookingEvent, plus a field this consumer does not know. */
    record ProducerBookingEvent(String eventId, String eventType, Instant occurredAt, Long bookingId, Long userId,
                                String userEmail, Long sessionId, Long activityId, String activityTitle,
                                LocalDateTime startTime, Integer participants, String bookingType,
                                BigDecimal totalPrice, String fieldAddedLater) {}

    @Test
    void producerJsonIsReadByTheConsumer() {
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 9, 30);
        ProducerBookingEvent sent = new ProducerBookingEvent("evt-1", "BOOKING_CONFIRMED", Instant.parse("2026-11-16T10:00:00Z"),
                10L, 5L, "user@test.com", 3L, 7L, "Kayaking", start, 2, "INDIVIDUAL", new BigDecimal("30.000"), "x");

        try (JacksonJsonSerializer<Object> serializer = new JacksonJsonSerializer<>();
             JacksonJsonDeserializer<BookingEvent> deserializer = new JacksonJsonDeserializer<>()) {
            serializer.configure(Map.of("spring.json.add.type.headers", false), false);
            deserializer.configure(Map.of(
                    "spring.json.use.type.headers", false,
                    "spring.json.value.default.type", BookingEvent.class.getName(),
                    "spring.json.trusted.packages", "com.bookngo.notificationservice.kafka"), false);

            byte[] json = serializer.serialize("booking-events", sent);
            BookingEvent received = deserializer.deserialize("booking-events", json);

            assertThat(received.eventId()).isEqualTo("evt-1");
            assertThat(received.eventType()).isEqualTo("BOOKING_CONFIRMED");
            assertThat(received.occurredAt()).isEqualTo(Instant.parse("2026-11-16T10:00:00Z"));
            assertThat(received.startTime()).isEqualTo(start);
            assertThat(received.totalPrice()).isEqualByComparingTo("30");
            assertThat(received.activityTitle()).isEqualTo("Kayaking");
        }
    }
}
