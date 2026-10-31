package ru.practicum.core.user.interservice;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.user.interservice.feign.ReviewFeignClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewInterserviceImpl implements ReviewInterservice {
    private final ReviewFeignClient reviewFeignClient;

    @Override
    public List<UserAverageScore> findAverageUsersRatingByIds(List<Long> ids) {
        try {
            return reviewFeignClient.findAverageUsersRatingByIds(ids);
        } catch (FeignException e) {
            switch (e.status()) {
                case 404 -> throw new NotFoundException(
                        "Average user ratings endpoint was not found in review service"
                );
                case 409 -> throw new ConflictException(String.format(
                        "Review service reported a conflict while retrieving average ratings for user IDs %s", ids
                ));
                default -> throw e;
            }
        }
    }
}
