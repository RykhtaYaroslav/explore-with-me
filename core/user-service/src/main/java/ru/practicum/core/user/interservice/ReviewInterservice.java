package ru.practicum.core.user.interservice;

import ru.practicum.core.common.dto.user.UserAverageScore;

import java.util.List;

public interface ReviewInterservice {
    List<UserAverageScore> findAverageUsersRatingByIds(List<Long> ids);
}
