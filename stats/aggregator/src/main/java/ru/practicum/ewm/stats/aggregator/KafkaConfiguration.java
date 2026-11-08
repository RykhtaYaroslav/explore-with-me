package ru.practicum.ewm.stats.aggregator;

import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.kafka.avro.AvroSpecificDeserializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfiguration {
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

    @Bean
    public ConsumerFactory<String, UserActionAvro> userActionConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.client-id}") String clientId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String autoOffsetReset,
            @Value("${spring.kafka.consumer.enable-auto-commit}") boolean enableAutoCommit,
            @Value("${spring.kafka.consumer.max-poll-records}") int maxPollRecords,
            @Value("${spring.kafka.consumer.max-poll-interval-ms}") int maxPollIntervalMs,
            @Value("${spring.kafka.consumer.session-timeout-ms}") int sessionTimeoutMs,
            @Value("${spring.kafka.consumer.heartbeat-interval-ms}") int heartbeatIntervalMs,
            @Value("${spring.kafka.consumer.fetch-min-bytes}") int fetchMinBytes,
            @Value("${spring.kafka.consumer.fetch-max-wait-ms}") int fetchMaxWaitMs
    ) {
        Map<String, Object> properties = consumerProperties(
                bootstrapServers,
                groupId,
                clientId,
                autoOffsetReset,
                enableAutoCommit,
                maxPollRecords,
                maxPollIntervalMs,
                sessionTimeoutMs,
                heartbeatIntervalMs,
                fetchMinBytes,
                fetchMaxWaitMs
        );
        return new DefaultKafkaConsumerFactory<>(
                properties,
                new ErrorHandlingDeserializer<>(new StringDeserializer()),
                new ErrorHandlingDeserializer<>(new AvroSpecificDeserializer<>(UserActionAvro.class))
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> kafkaListenerContainerFactory(
            ConsumerFactory<String, UserActionAvro> userActionConsumerFactory,
            @Value("${spring.kafka.listener.poll-timeout}") Duration pollTimeout
    ) {
        ConcurrentKafkaListenerContainerFactory<String, UserActionAvro> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(userActionConsumerFactory);
        factory.getContainerProperties().setPollTimeout(pollTimeout.toMillis());
        factory.setCommonErrorHandler(invalidMessageErrorHandler());
        return factory;
    }

    /**
     * Повторяет сбои с паузой, но не повторяет сообщения, не прошедшие валидацию:
     * они пропускаются с записью в лог и не блокируют партицию.
     */
    private static DefaultErrorHandler invalidMessageErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(new FixedBackOff(1_000L, Long.MAX_VALUE));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }

    private Map<String, Object> consumerProperties(
            String bootstrapServers,
            String groupId,
            String clientId,
            String autoOffsetReset,
            boolean enableAutoCommit,
            int maxPollRecords,
            int maxPollIntervalMs,
            int sessionTimeoutMs,
            int heartbeatIntervalMs,
            int fetchMinBytes,
            int fetchMaxWaitMs
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);
        properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);
        return properties;
    }
}
