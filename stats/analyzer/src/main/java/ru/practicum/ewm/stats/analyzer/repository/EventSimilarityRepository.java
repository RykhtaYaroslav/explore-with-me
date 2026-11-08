package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;

import java.util.List;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarityEntity, EventSimilarityId> {
    @Query(value = """
            SELECT candidate.event_id AS "eventId", MAX(candidate.score) AS score
            FROM (
                SELECT event_b AS event_id, score FROM event_similarities WHERE event_a IN (:sourceIds)
                UNION ALL
                SELECT event_a AS event_id, score FROM event_similarities WHERE event_b IN (:sourceIds)
            ) candidate
            WHERE NOT EXISTS (
                SELECT 1 FROM user_event_interactions history
                WHERE history.user_id = :userId AND history.event_id = candidate.event_id
            )
            GROUP BY candidate.event_id
            ORDER BY score DESC, candidate.event_id
            LIMIT :limit
            """, nativeQuery = true)
    List<EventScore> findUnseenCandidates(long userId, List<Long> sourceIds, int limit);

    @Query(value = """
            SELECT ranked.event_id AS "eventId",
                   SUM(ranked.weight * ranked.similarity) / SUM(ranked.similarity) AS score
            FROM (
                SELECT neighbor.*,
                       ROW_NUMBER() OVER (
                           PARTITION BY neighbor.event_id
                           ORDER BY neighbor.similarity DESC, neighbor.history_event_id
                       ) AS neighbor_rank
                FROM (
                    SELECT pair.event_a AS event_id, pair.event_b AS history_event_id,
                           history.weight, pair.score AS similarity
                    FROM event_similarities pair
                    JOIN user_event_interactions history
                        ON history.user_id = :userId AND history.event_id = pair.event_b
                    WHERE pair.event_a IN (:candidateIds)
                    UNION ALL
                    SELECT pair.event_b AS event_id, pair.event_a AS history_event_id,
                           history.weight, pair.score AS similarity
                    FROM event_similarities pair
                    JOIN user_event_interactions history
                        ON history.user_id = :userId AND history.event_id = pair.event_a
                    WHERE pair.event_b IN (:candidateIds)
                ) neighbor
                WHERE neighbor.similarity > 0
            ) ranked
            WHERE ranked.neighbor_rank <= :neighborLimit
            GROUP BY ranked.event_id
            ORDER BY score DESC, ranked.event_id
            LIMIT :limit
            """, nativeQuery = true)
    List<EventScore> predictScores(long userId, List<Long> candidateIds, int neighborLimit, int limit);

    interface EventScore {
        long getEventId();

        double getScore();
    }
}
