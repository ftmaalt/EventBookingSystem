package com.bookngo.bookingservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Placeholder consumer. Runs only after the booking transaction commits.
 * When Kafka is added, replace the body with kafkaTemplate.send("booking-events", ...).
 */
@Component
public class BookingEventLogger {

    private static final Logger logger = LoggerFactory.getLogger(BookingEventLogger.class);

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingNotificationEvent event) {
        logger.info("Booking event {} -> bookingId={}, userId={}, sessionId={}",
                event.type(), event.bookingId(), event.userId(), event.sessionId());
    }
}
