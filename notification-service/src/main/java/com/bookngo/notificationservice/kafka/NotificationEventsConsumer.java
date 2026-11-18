package com.bookngo.notificationservice.kafka;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;
import com.bookngo.notificationservice.model.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Kafka CONSUMER for booking-events (from booking-service) and account-events (from auth-service).
 * It turns each domain event into a NotificationEventRequest and hands it to the same dispatcher the
 * REST endpoint uses. Failures are retried and then sent to "<topic>-dlt" (see KafkaConsumerConfig).
 * Event types this service does not care about are skipped, so producers can add new types freely.
 */
@Component
public class NotificationEventsConsumer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationEventsConsumer.class);
    private static final DateTimeFormatter START_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IdempotentEventHandler handler;

    public NotificationEventsConsumer(IdempotentEventHandler handler) {
        this.handler = handler;
    }

    @KafkaListener(id = "notification-booking-events", idIsGroup = false, topics = "${app.kafka.topics.booking-events}",
            properties = "spring.json.value.default.type=com.bookngo.notificationservice.kafka.BookingEvent")
    public void onBookingEvent(@Payload BookingEvent event,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        logger.info("Consumed {} eventId={} bookingId={} from {}-{}@{}",
                event.eventType(), event.eventId(), event.bookingId(), topic, partition, offset);

        NotificationType type = switch (String.valueOf(event.eventType())) {
            case "BOOKING_CONFIRMED" -> NotificationType.BOOKING_CONFIRMED;
            case "BOOKING_CANCELLED" -> NotificationType.BOOKING_CANCELLED;
            case "WAITLIST_ALLOCATED" -> NotificationType.WAITLIST_ALLOCATED;
            default -> null;
        };
        if (type == null) {
            logger.info("Ignoring booking event type {} (eventId={})", event.eventType(), event.eventId());
            return;
        }
        handler.handleOnce(event.eventId(), topic, new NotificationEventRequest(
                type, event.userId(), event.userEmail(), event.bookingId(), event.activityTitle(),
                event.startTime() == null ? null : event.startTime().format(START_TIME),
                event.participants(), event.bookingType(), event.totalPrice(),
                null, null, null, null, null, null));
    }

    @KafkaListener(id = "notification-account-events", idIsGroup = false, topics = "${app.kafka.topics.account-events}",
            properties = "spring.json.value.default.type=com.bookngo.notificationservice.kafka.AccountEvent")
    public void onAccountEvent(@Payload AccountEvent event,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset) {
        logger.info("Consumed {} eventId={} userId={} from {}-{}@{}",
                event.eventType(), event.eventId(), event.recipientUserId(), topic, partition, offset);

        NotificationType type = switch (String.valueOf(event.eventType())) {
            case "USER_REGISTERED" -> NotificationType.EMAIL_VERIFICATION;
            case "PASSWORD_RESET_REQUESTED" -> NotificationType.PASSWORD_RESET;
            case "PROVIDER_APPLICATION_SUBMITTED" -> NotificationType.NEW_PROVIDER_APPLICATION;
            case "PROVIDER_APPLICATION_APPROVED" -> NotificationType.PROVIDER_APPLICATION_APPROVED;
            case "PROVIDER_APPLICATION_REJECTED" -> NotificationType.PROVIDER_APPLICATION_REJECTED;
            default -> null;
        };
        if (type == null) {
            logger.info("Ignoring account event type {} (eventId={})", event.eventType(), event.eventId());
            return;
        }
        handler.handleOnce(event.eventId(), topic, new NotificationEventRequest(
                type, event.recipientUserId(), event.recipientEmail(),
                null, null, null, null, null, null,
                event.reviewNote(), event.token(), event.businessName(), event.contactName(),
                event.city(), event.proposedActivities()));
    }
}
