package ru.practicum.ewm.stats.aggregator;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.concurrent.CompletableFuture;

/** Publishes updated event-pair similarity scores to Analyzer through Kafka. */
public interface EventSimilarityPublisher {
    CompletableFuture<?> publish(EventSimilarityAvro similarity);
}
