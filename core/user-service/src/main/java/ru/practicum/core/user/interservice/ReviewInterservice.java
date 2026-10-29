package ru.practicum.core.user.interservice;

import ru.practicum.core.common.dto.user.UserAverageScore;

import java.util.List;

public interface ReviewInterservice {
    List<UserAverageScore> findAverageScoresByTargetIds(List<Long> ids);
}
