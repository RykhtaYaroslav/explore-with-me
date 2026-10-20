package ru.practicum.main.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.dto.EventRatingCount;
import ru.practicum.main.review.model.EventReview;
import ru.practicum.main.review.model.EventReviewId;

import java.util.List;

public interface EventReviewRepository extends JpaRepository<EventReview, EventReviewId> {

    @Query("""
            SELECT new ru.practicum.main.event.dto.EventRatingCount(er.event.id, AVG(er.score))
            FROM EventReview er
            WHERE er.event.id IN :eventIds
            GROUP BY er.event.id
            """)
    List<EventRatingCount> countRatingByIds(@Param("eventIds") List<Long> eventIds);

    @Query("""
            SELECT COUNT(er) > 0
            FROM EventReview er
            WHERE er.rater.id = :raterId
              AND er.event.id = :eventId
            """)
    boolean existsByParams(
            @Param("raterId") long raterId,
            @Param("eventId") long eventId
    );

}
