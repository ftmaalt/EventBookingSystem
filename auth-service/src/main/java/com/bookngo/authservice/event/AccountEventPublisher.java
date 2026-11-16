package com.bookngo.authservice.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kafka PRODUCER for the account-events topic.
 * Runs only AFTER the auth transaction commits: a failed registration never sends a verification e-mail,
 * and registering does not wait for an SMTP server. The recipient's user id is the message key,
 * so events for one user stay in order.
 */
@Component
public class AccountEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AccountEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String topic;

    public AccountEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                                 @Value("${app.kafka.topics.account-events}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AccountEvent event) {
        kafkaTemplate.send(topic, String.valueOf(event.recipientUserId()), event).whenComplete((result, ex) -> {
            if (ex != null) {
                logger.error("Failed to publish {} eventId={} userId={} to {}",
                        event.eventType(), event.eventId(), event.recipientUserId(), topic, ex);
            } else {
                logger.info("Published {} eventId={} userId={} to {}-{}@{}",
                        event.eventType(), event.eventId(), event.recipientUserId(), topic,
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
