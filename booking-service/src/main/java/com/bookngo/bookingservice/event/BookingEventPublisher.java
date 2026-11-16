package com.bookngo.bookingservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kafka PRODUCER for the booking-events topic.
 * Runs only AFTER the booking transaction commits, so a rolled-back booking never produces an event,
 * and the HTTP request does not wait for the broker. The bookingId is the message key, so every event
 * for one booking lands on the same partition and is consumed in order.
 */
@Component
public class BookingEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public BookingEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${app.kafka.topics.booking-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(BookingEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.bookingId()), event).whenComplete((result, ex) -> {
            if (ex != null) {
                // booking is already committed
                logger.error("Failed to publish {} eventId={} bookingId={} to {}",
                        event.eventType(), event.eventId(), event.bookingId(), topic, ex);
            } else {
                logger.info("Published {} eventId={} bookingId={} to {}-{}@{}",
                        event.eventType(), event.eventId(), event.bookingId(), topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
