package com.bookngo.bookingservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic bookingEventsTopic(@Value("${app.kafka.topics.booking-events}") String name,
                                       @Value("${app.kafka.partitions:3}") int partitions,
                                       @Value("${app.kafka.replication-factor:1}") short replicas) {
        return TopicBuilder.name(name).partitions(partitions).replicas(replicas).build();
    }
}
