package ru.practicum.core.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.repository.UserReviewRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewInterserviceImpl implements ReviewInterservice {
    private final EventReviewRepository eventReviewRepository;
    private final UserReviewRepository userReviewRepository;
    // TODO rename interface + implementation

    @Override
    public List<EventRatingCount> getEventsRatingsByIds(List<Long> ids) {
        return eventReviewRepository.getRatingByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAverageScore> findAverageUsersRatingByIds(List<Long> ids) {
        Map<Long, Double> averageScoresByUserId = userReviewRepository.findAverageScoresByTargetIds(ids).stream()
                .collect(Collectors.toMap(UserAverageScore::targetId, UserAverageScore::averageScore));

        return ids.stream()
                .distinct()
                .map(userId -> new UserAverageScore(userId, averageScoresByUserId.getOrDefault(userId, 0.0)))
                .toList();
    }
}
