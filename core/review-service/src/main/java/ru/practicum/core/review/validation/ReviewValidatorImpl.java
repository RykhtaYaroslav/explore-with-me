package ru.practicum.core.review.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.common.time.TimeProvider;
import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.review.interservice.EventInterservice;
import ru.practicum.core.review.interservice.ParticipationInterservice;
import ru.practicum.core.review.interservice.UserInterservice;
import ru.practicum.core.review.repository.EventReviewRepository;
import ru.practicum.core.review.repository.UserReviewRepository;
import ru.practicum.core.user.dto.UserShortDto;

import java.time.LocalDateTime;

import static ru.practicum.core.common.util.EwmConstants.HOURS_OFFSET_FOR_REVIEWS_CREATING;

@Component
@RequiredArgsConstructor
public class ReviewValidatorImpl implements ReviewValidator {
    private final UserInterservice userInterservice;
    private final EventInterservice eventInterservice;
    private final ParticipationInterservice participationInterservice;
    private final UserReviewRepository userReviewRepository;
    private final EventReviewRepository eventReviewRepository;
    private final TimeProvider timeProvider;

    @Override
    public UserShortDto findUserOrThrow(Long userId) {
        if (!userInterservice.existsById(userId)) {
            throw new NotFoundException(String.format("User with id = %d not found", userId));
        }
        return userInterservice.getShortById(userId);
    }

    @Override
    public EventShortDto findEventOrThrow(Long eventId) {
        if (!eventInterservice.existsById(eventId)) {
            throw new NotFoundException(String.format("Event with id = %d not found", eventId));
        }
        return eventInterservice.getReviewInfo(eventId);
    }

    @Override
    public void validateEventReview(Long raterId, EventShortDto event) {
        validateDateTime(event.getEventDate());
        validateParticipation(event.getId(), raterId);

        if (eventReviewRepository.existsByParams(raterId, event.getId())) {
            throw new ConflictException(String.format(
                    "User with id = %d has already reviewed event with id = %d", raterId, event.getId()));
        }
    }

    @Override
    public void validateAuthorReview(Long raterId, Long authorId, EventShortDto event) {
        if (!eventInterservice.isOrganizer(event.getId(), authorId)) {
            throw new ConflictException(String.format("User with id = %d is not the organizer of event with id = %d",
                    authorId, event.getId()));
        }

        validateDateTime(event.getEventDate());
        validateParticipation(event.getId(), raterId);

        if (userReviewRepository.existsByParams(raterId, event.getId(), authorId)) {
            throw new ConflictException(String.format(
                    "User with id = %d has already reviewed organizer with id = %d for event with id = %d",
                    raterId, authorId, event.getId()));
        }
    }

    private void validateDateTime(LocalDateTime eventDate) {
        LocalDateTime allowedTime = eventDate.plusHours(HOURS_OFFSET_FOR_REVIEWS_CREATING);
        if (timeProvider.now().isBefore(allowedTime)) {
            throw new ConflictException(String.format(
                    "An event can only be reviewed %d hours after it starts",
                    HOURS_OFFSET_FOR_REVIEWS_CREATING));
        }
    }

    private void validateParticipation(Long eventId, Long userId) {
        if (!participationInterservice.isConfirmedParticipant(userId, eventId)) {
            throw new ConflictException(String.format(
                    "User with id = %d is not a participant of event with id = %d", userId, eventId));
        }
    }
}
