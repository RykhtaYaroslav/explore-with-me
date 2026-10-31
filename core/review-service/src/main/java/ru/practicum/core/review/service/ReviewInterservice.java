package ru.practicum.core.review.service;

import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.event.dto.EventRatingCount;

import java.util.List;

public interface ReviewInterservice {
    List<EventRatingCount> getEventsRatingsByIds(List<Long> ids);

    List<UserAverageScore> findAverageUsersRatingByIds(List<Long> ids);
}
