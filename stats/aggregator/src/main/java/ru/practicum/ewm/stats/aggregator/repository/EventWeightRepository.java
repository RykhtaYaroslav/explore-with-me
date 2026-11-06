package ru.practicum.ewm.stats.aggregator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.aggregator.model.EventWeightEntity;

@SuppressWarnings("all")
public interface EventWeightRepository extends JpaRepository<EventWeightEntity, Long> {

    /** Backfills only events that have no aggregate row yet, preserving later incremental totals. */
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO aggregator_event_weights (event_id, total_weight)
            SELECT event_id, SUM(weight)
            FROM aggregator_user_event_interactions
            GROUP BY event_id
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    void backfillMissingWeights();

    /** Atomically adds a positive interaction delta, creating the event row when necessary. */
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO aggregator_event_weights (event_id, total_weight)
            VALUES (:eventId, :delta)
            ON CONFLICT (event_id) DO UPDATE
            SET total_weight = aggregator_event_weights.total_weight + EXCLUDED.total_weight
            """, nativeQuery = true)
    void addWeight(@Param("eventId") long eventId, @Param("delta") double delta);
}
