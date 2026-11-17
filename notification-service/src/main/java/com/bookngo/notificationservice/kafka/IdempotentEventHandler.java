package com.bookngo.notificationservice.kafka;

import com.bookngo.notificationservice.dto.request.NotificationEventRequest;
import com.bookngo.notificationservice.model.ProcessedEvent;
import com.bookngo.notificationservice.repository.ProcessedEventRepository;
import com.bookngo.notificationservice.service.NotificationDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Kafka delivers at-least-once, so the same event can arrive twice.
 * The "processed" marker and the in-app notification are
 * written in ONE transaction
 * either both are stored or neither is, so a retried message is either
 * processed for the first time or recognized as a duplicate and skipped.
 */
@Service
public class IdempotentEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(IdempotentEventHandler.class);

    private final ProcessedEventRepository processedEventRepository;
    private final NotificationDispatcher dispatcher;

    public IdempotentEventHandler(ProcessedEventRepository processedEventRepository, NotificationDispatcher dispatcher) {
        this.processedEventRepository = processedEventRepository;
        this.dispatcher = dispatcher;
    }

    @Transactional
    public void handleOnce(String eventId, String topic, NotificationEventRequest request) {
        if (eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("Event from " + topic + " has no eventId.");
        }
        if (processedEventRepository.existsById(eventId)) {
            logger.info("Skipping duplicate event {} from {}", eventId, topic);
            return;
        }
        // A concurrent duplicate fails here on the primary key, rolls back, and is skipped on retry.
        processedEventRepository.saveAndFlush(new ProcessedEvent(eventId, topic));
        dispatcher.dispatch(request);
    }
}
