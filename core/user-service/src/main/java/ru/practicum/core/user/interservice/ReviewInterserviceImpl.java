package ru.practicum.core.user.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.user.interservice.feign.ReviewFeignClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewInterserviceImpl implements ReviewInterservice {
    private final ReviewFeignClient reviewFeignClient;

    @Override
    public List<UserAverageScore> findAverageScoresByTargetIds(List<Long> ids) {
        return List.of();
    }
}
