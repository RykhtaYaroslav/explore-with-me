package ru.practicum.core.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.review.model.UserReview;

import java.util.List;

public interface UserReviewRepository extends JpaRepository<UserReview, Long> {
    List<UserReview> findAllByTargetIdOrderByIdDesc(Long targetId);

    @Query("""
            SELECT new ru.practicum.core.common.dto.user.UserAverageScore(ur.targetId, AVG(ur.score))
            FROM UserReview ur
            WHERE ur.targetId IN :targetIds
            GROUP BY ur.targetId
            """)
    List<UserAverageScore> findAverageScoresByTargetIds(@Param("targetIds") List<Long> targetIds);

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
