package ru.practicum.core.review.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;
import ru.practicum.core.review.service.ReviewService;

@RestController
@RequestMapping("/rating/{raterId}")
@RequiredArgsConstructor
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/author/{eventId}/{authorId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OutputUserReviewDto createAuthorReview(@PathVariable @Positive(message = "Rater id must be positive") Long raterId,
                                                  @PathVariable @Positive(message = "Event id must be positive") Long eventId,
                                                  @PathVariable @Positive(message = "Author id must be positive") Long authorId,
                                                  @Valid @RequestBody NewUserReviewDto request) {
        return reviewService.createAuthorReview(raterId, eventId, authorId, request);
    }

    @PostMapping("/eventId/{eventId}")
    @ResponseStatus(HttpStatus.CREATED)
    public OutputEventReviewDto createEventReview(@PathVariable @Positive(message = "Rater id must be positive") Long raterId,
                                                  @PathVariable @Positive(message = "Event id must be positive") Long eventId,
                                                  @RequestBody @Valid NewEventReviewDto request) {
        return reviewService.createEventReview(raterId, eventId, request);
    }
}
