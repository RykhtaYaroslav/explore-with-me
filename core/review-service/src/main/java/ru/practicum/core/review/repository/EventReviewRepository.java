package ru.practicum.core.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.review.model.EventReview;

import java.util.List;

public interface EventReviewRepository extends JpaRepository<EventReview, Long> {

    @Query("""
            SELECT new ru.practicum.core.event.dto.EventRatingCount(er.eventId, AVG(er.score))
            FROM EventReview er
            WHERE er.eventId IN :eventIds
            GROUP BY er.eventId
            """)
    List<EventRatingCount> countRatingByIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
            SELECT COUNT(er) > 0
            FROM EventReview er
            WHERE er.raterId = :raterId
              AND er.eventId = :eventId
            """)
    boolean existsByParams(
            @Param("raterId") long raterId,
            @Param("eventId") long eventId
    );

}
