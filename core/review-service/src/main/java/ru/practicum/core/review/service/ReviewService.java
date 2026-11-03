package ru.practicum.core.review.service;

import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;

import java.util.List;

public interface ReviewService {
    OutputUserReviewDto createAuthorReview(Long raterId, Long eventId, Long authorId, NewUserReviewDto request);

    OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request);

    List<OutputEventReviewDto> getEventReviews(Long eventId);

    List<OutputUserReviewDto> getAuthorReviews(Long authorId);
}
