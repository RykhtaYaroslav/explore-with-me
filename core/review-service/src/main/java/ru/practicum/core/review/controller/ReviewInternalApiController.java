package ru.practicum.core.review.controller;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.common.dto.user.UserAverageScore;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.review.service.internalapi.ReviewInternalApiService;

import java.util.List;

@RestController
@RequestMapping("/interservice/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewInternalApiController {
    private final ReviewInternalApiService reviewInternalApiService;

    @PostMapping("/ratings")
    public List<EventRatingCount> getEventsRatingsByIds(@RequestBody @NotEmpty List<@Positive Long> ids) {
        return reviewInternalApiService.getEventsRatingsByIds(ids); //TODO fix endpoint uri
    }

    @PostMapping("users")
    public List<UserAverageScore> findAverageUsersRatingByIds(@RequestBody @NotEmpty List<@Positive Long> ids) {
        return reviewInternalApiService.findAverageUsersRatingByIds(ids);
    }
}
