package ru.practicum.ewm.stats.analyzer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.avro.AvroSpecificDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {
    @Bean
    public ConsumerFactory<String, UserActionAvro> userActionConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${stats.kafka.consumer.user-actions-group-id}") String groupId
    ) {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(bootstrapServers, groupId),
                new StringDeserializer(),
                new AvroSpecificDeserializer<>(UserActionAvro.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro>
    userActionKafkaListenerContainerFactory(ConsumerFactory<String, UserActionAvro> userActionConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, EventSimilarityAvro> eventSimilarityConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${stats.kafka.consumer.event-similarity-group-id}") String groupId
    ) {
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties(bootstrapServers, groupId),
                new StringDeserializer(),
                new AvroSpecificDeserializer<>(EventSimilarityAvro.class)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro>
    eventSimilarityKafkaListenerContainerFactory(
            ConsumerFactory<String, EventSimilarityAvro> eventSimilarityConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, EventSimilarityAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(eventSimilarityConsumerFactory);
        return factory;
    }

    private Map<String, Object> consumerProperties(String bootstrapServers, String groupId) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        return properties;
    }
}
