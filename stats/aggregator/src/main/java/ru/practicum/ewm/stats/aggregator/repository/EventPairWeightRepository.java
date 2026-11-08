package ru.practicum.ewm.stats.aggregator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.aggregator.model.EventPairWeightEntity;
import ru.practicum.ewm.stats.aggregator.model.EventPairWeightId;

import java.util.List;

public interface EventPairWeightRepository extends JpaRepository<EventPairWeightEntity, EventPairWeightId> {

    /**
     * Returns every pair involving the updated event and both event totals needed for scoring.
     */
    @Query(value = """
            SELECT pair.event_a AS "eventA",
                   pair.event_b AS "eventB",
                   pair.shared_weight AS "sharedWeight",
                   first_event.total_weight AS "eventAWeight",
                   second_event.total_weight AS "eventBWeight"
            FROM aggregator_event_pair_weights pair
            JOIN aggregator_event_weights first_event ON first_event.event_id = pair.event_a
            JOIN aggregator_event_weights second_event ON second_event.event_id = pair.event_b
            WHERE pair.event_a = :eventId OR pair.event_b = :eventId
            """, nativeQuery = true)
    List<SimilarityAggregate> findSimilaritiesForEvent(@Param("eventId") long eventId);

    interface SimilarityAggregate {
        Long getEventA();

        Long getEventB();

        Double getSharedWeight();

        Double getEventAWeight();

        Double getEventBWeight();
    }

    /**
     * Backfills only missing event pairs from the existing user interaction rows.
     */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO aggregator_event_pair_weights (event_a, event_b, shared_weight)
            SELECT first_event.event_id, second_event.event_id,
                   SUM(LEAST(first_event.weight, second_event.weight))
            FROM aggregator_user_event_interactions first_event
            JOIN aggregator_user_event_interactions second_event
              ON first_event.user_id = second_event.user_id
             AND first_event.event_id < second_event.event_id
            GROUP BY first_event.event_id, second_event.event_id
            ON CONFLICT (event_a, event_b) DO NOTHING
            """, nativeQuery = true)
    void backfillMissingPairs();

    /**
     * Одним запросом добавляет положительные дельты всем парам мероприятия пользователя; newWeight > oldWeight.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO aggregator_event_pair_weights (event_a, event_b, shared_weight)
            SELECT LEAST(:eventId, interaction.event_id),
                   GREATEST(:eventId, interaction.event_id),
                   LEAST(:newWeight, interaction.weight) - LEAST(:oldWeight, interaction.weight)
            FROM aggregator_user_event_interactions interaction
            WHERE interaction.user_id = :userId
              AND interaction.event_id <> :eventId
              AND LEAST(:newWeight, interaction.weight) - LEAST(:oldWeight, interaction.weight) > 0
            ON CONFLICT (event_a, event_b) DO UPDATE
            SET shared_weight = aggregator_event_pair_weights.shared_weight + EXCLUDED.shared_weight
            """, nativeQuery = true)
    void updateSharedWeights(@Param("userId") long userId,
                             @Param("eventId") long eventId,
                             @Param("oldWeight") double oldWeight,
                             @Param("newWeight") double newWeight);
}
