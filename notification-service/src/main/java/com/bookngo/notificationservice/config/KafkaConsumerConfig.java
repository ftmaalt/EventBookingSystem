package com.bookngo.notificationservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DelegatingByTypeSerializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 *  - a failing message is retried with exponential back-off (1s, 2s, 4s) in case the cause is temporary
 *    (database restart, network blip);
 *  - after the last retry, or straight away for errors a retry cannot fix (bad JSON, invalid data),
 *    the message is published to "<topic>-dlt" so it is not lost and does not block the partition.
 * Spring Boot applies this error handler to every @KafkaListener automatically.
 */
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> deadLetterTemplate,
                                                 @Value("${app.kafka.retry.attempts:3}") int attempts) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(deadLetterTemplate);
        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0);
        backOff.setMaxAttempts(attempts);
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);
        // retrying these would give the same result every time
        handler.addNotRetryableExceptions(IllegalArgumentException.class, NullPointerException.class);
        return handler;
    }

    /**
     * Publishes dead letters. A record that failed deserialization only exists as raw bytes,
     * so byte[] is written as-is and anything else as JSON.
     */
    @Bean
    public KafkaTemplate<Object, Object> deadLetterTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<Class<?>, org.apache.kafka.common.serialization.Serializer<?>> byType = new LinkedHashMap<>();
        byType.put(byte[].class, new ByteArraySerializer());
        byType.put(Object.class, new JacksonJsonSerializer<>().noTypeInfo());
        Map<String, Object> config = Map.of(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                ProducerConfig.ACKS_CONFIG, "all",
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<Object, Object> factory = new DefaultKafkaProducerFactory<>(config);
        factory.setValueSerializer(new DelegatingByTypeSerializer(byType, true));
        return new KafkaTemplate<>(factory);
    }

    // dead-letter topics need at least as many partitions as their source topic (same partition is used).
    @Bean
    public NewTopic bookingEventsDeadLetterTopic(@Value("${app.kafka.topics.booking-events}") String source,
                                                 @Value("${app.kafka.partitions:3}") int partitions,
                                                 @Value("${app.kafka.replication-factor:1}") short replicas) {
        return TopicBuilder.name(source + "-dlt").partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic accountEventsDeadLetterTopic(@Value("${app.kafka.topics.account-events}") String source,
                                                 @Value("${app.kafka.partitions:3}") int partitions,
                                                 @Value("${app.kafka.replication-factor:1}") short replicas) {
        return TopicBuilder.name(source + "-dlt").partitions(partitions).replicas(replicas).build();
    }
}
