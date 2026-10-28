package ru.practicum.core.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.core.review.model.UserReview;

import java.util.List;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {
    List<UserReview> findAllByTargetIdOrderByIdDesc(Long targetId);

    @Query("select ur.targetId, AVG(ur.score) from UserReview ur " +
            "where ur.targetId in :targetIds group by ur.targetId")
    List<Object[]> findAverageScoresByTargetIds(@Param("targetIds") List<Long> targetIds);
    // TODO исправить

    @Query("""
            SELECT COUNT(ur) > 0
            FROM UserReview ur
            WHERE ur.raterId = :raterId
              AND ur.eventId = :eventId
              AND ur.targetId = :targetId
            """)
    boolean existsByParams(
            @Param("raterId") long raterId,
            @Param("eventId") long eventId,
            @Param("targetId") long targetId
    );
}
