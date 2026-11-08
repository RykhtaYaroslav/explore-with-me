package ru.practicum.ewm.stats.aggregator;

import ru.practicum.ewm.stats.avro.UserActionAvro;

/**
 * Processes user actions and incrementally updates event and event-pair weight sums.
 */
public interface UserActionSimilarityProcessor {

    /**
     * Retains the user's highest weight for the event, updates affected partial sums, recalculates
     * the scores for affected pairs, and publishes those scores to Analyzer through Kafka.
     *
     * @param action the user action received from Kafka
     * @throws RuntimeException if persistence or Kafka publishing fails, allowing the Kafka
     *                          listener to retry the record
     */
    void process(UserActionAvro action);
}
