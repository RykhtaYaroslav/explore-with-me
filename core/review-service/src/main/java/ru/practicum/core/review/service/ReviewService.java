package ru.practicum.core.review.service;

import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;

public interface ReviewService {
    OutputUserReviewDto createAuthorReview(Long raterId, Long eventId, Long authorId, NewUserReviewDto request);

    OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request);
}
