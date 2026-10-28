package ru.practicum.core.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.core.review.model.UserReview;
import ru.practicum.core.review.model.UserReviewId;

import java.util.List;

public interface UserReviewRepository extends JpaRepository<UserReview, UserReviewId> {
    @Query("select ur.targetId.id, AVG(ur.score) from UserReview ur " +
            "where ur.targetId.id in :targetIds group by ur.targetId.id")
    List<Object[]> findAverageScoresByTargetIds(@Param("targetIds") List<Long> targetIds);

    @Query("""
            SELECT COUNT(ur) > 0
            FROM UserReview ur
            WHERE ur.rater.id = :raterId
              AND ur.event.id = :eventId
              AND ur.targetId.id = :targetId
            """)
    boolean existsByParams(
            @Param("raterId") long raterId,
            @Param("eventId") long eventId,
            @Param("targetId") long targetId
    );
}
