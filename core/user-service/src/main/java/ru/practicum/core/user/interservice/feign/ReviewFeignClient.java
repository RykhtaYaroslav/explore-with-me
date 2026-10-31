package ru.practicum.core.user.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.common.dto.user.UserAverageScore;

import java.util.List;

@FeignClient(name = ServiceNames.REVIEW_SERVICE)
public interface ReviewFeignClient {

    @GetMapping
    List<UserAverageScore> findAverageUsersRatingByIds(@RequestBody List<Long> ids);
}
