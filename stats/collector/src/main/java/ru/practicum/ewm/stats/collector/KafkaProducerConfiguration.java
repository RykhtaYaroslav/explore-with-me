package ru.practicum.ewm.stats.collector;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfiguration {
    @Bean
    public ProducerFactory<String, SpecificRecord> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.producer.key-serializer}") String keySerializer,
            @Value("${spring.kafka.producer.value-serializer}") String valueSerializer,
            @Value("${spring.kafka.producer.acks}") String acks,
            @Value("${spring.kafka.producer.retries}") int retries,
            @Value("${spring.kafka.producer.properties.enable.idempotence}") boolean enableIdempotence,
            @Value("${spring.kafka.producer.properties.delivery.timeout.ms}") int deliveryTimeoutMs,
            @Value("${spring.kafka.producer.properties.request.timeout.ms}") int requestTimeoutMs,
            @Value("${spring.kafka.producer.properties.linger.ms}") int lingerMs,
            @Value("${spring.kafka.producer.properties.batch.size}") int batchSize,
            @Value("${spring.kafka.producer.properties.buffer.memory}") long bufferMemory,
            @Value("${spring.kafka.producer.client-id}") String clientId,
            @Value("${spring.kafka.producer.properties.max.request.size}") int maxRequestSize
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        properties.put(ProducerConfig.ACKS_CONFIG, acks);
        properties.put(ProducerConfig.RETRIES_CONFIG, retries);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, enableIdempotence);
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        properties.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMs);
        properties.put(ProducerConfig.LINGER_MS_CONFIG, lingerMs);
        properties.put(ProducerConfig.BATCH_SIZE_CONFIG, batchSize);
        properties.put(ProducerConfig.BUFFER_MEMORY_CONFIG, bufferMemory);
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ProducerConfig.MAX_REQUEST_SIZE_CONFIG, maxRequestSize);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, SpecificRecord> kafkaTemplate(
            ProducerFactory<String, SpecificRecord> producerFactory,
            @Value("${spring.kafka.template.default-topic}") String defaultTopic
    ) {
        KafkaTemplate<String, SpecificRecord> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setDefaultTopic(defaultTopic);
        return kafkaTemplate;
    }
}
