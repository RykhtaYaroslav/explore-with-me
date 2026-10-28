package ru.practicum.core.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.dto.NewEventReviewDto;
import ru.practicum.core.review.dto.NewUserReviewDto;
import ru.practicum.core.review.dto.OutputEventReviewDto;
import ru.practicum.core.review.dto.OutputUserReviewDto;
import ru.practicum.core.review.mapper.ReviewMapper;
import ru.practicum.core.review.model.EventReview;
import ru.practicum.core.review.model.UserReview;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.repository.UserReviewRepository;
import ru.practicum.core.review.validation.ReviewValidator;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {
    private final ReviewValidator reviewValidator;

    private final UserReviewRepository userReviewRepository;
    private final EventReviewRepository eventReviewRepository;

    private final ReviewMapper reviewMapper;

    @Override
    public OutputUserReviewDto createAuthorReview(Long raterId, Long eventId, Long authorId,
                                                  NewUserReviewDto request) {
        UserShortDto rater = reviewValidator.findUserOrThrow(raterId);
        UserShortDto author = reviewValidator.findUserOrThrow(authorId);
        EventShortDto event = reviewValidator.findEventOrThrow(eventId);
        reviewValidator.validateAuthorReview(raterId, authorId, event);

        UserReview review = reviewMapper.toUserReview(request, raterId, authorId, eventId);
        review = userReviewRepository.save(review);

        return reviewMapper.toUserReviewOutDto(review, rater, author, event);
    }

    @Override
    public OutputEventReviewDto createEventReview(Long raterId, Long eventId, NewEventReviewDto request) {
        UserShortDto rater = reviewValidator.findUserOrThrow(raterId);
        EventShortDto event = reviewValidator.findEventOrThrow(eventId);
        reviewValidator.validateEventReview(raterId, event);

        EventReview review = reviewMapper.toEventReview(request, raterId, eventId);
        review = eventReviewRepository.save(review);

        return reviewMapper.toEventReviewOutDto(review, rater, event);
    }

}
