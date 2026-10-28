package ru.practicum.core.review.controller;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;
import ru.practicum.core.review.service.ReviewService;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Validated
public class PublicReviewController {
    private final ReviewService reviewService;

    @GetMapping("/events/{eventId}")
    public List<OutputEventReviewDto> getEventReviews(@PathVariable @Positive Long eventId) {
        return reviewService.getEventReviews(eventId);
    }

    @GetMapping("/authors/{authorId}")
    public List<OutputUserReviewDto> getAuthorReviews(@PathVariable @Positive Long authorId) {
        return reviewService.getAuthorReviews(authorId);
    }
}
