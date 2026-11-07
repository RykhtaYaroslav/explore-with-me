package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka implementation that partitions similarity scores by their canonical event pair.
 */
@Component
@RequiredArgsConstructor
public class EventSimilarityPublisherImpl implements EventSimilarityPublisher {
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;

    @Override
    public CompletableFuture<?> publish(EventSimilarityAvro similarity) {
        String key = similarity.getEventA() + ":" + similarity.getEventB();
        return kafkaTemplate.sendDefault(key, similarity);
    }
}
