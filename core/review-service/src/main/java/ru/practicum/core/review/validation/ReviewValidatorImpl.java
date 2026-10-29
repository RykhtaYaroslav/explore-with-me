package ru.practicum.core.review.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.exception.ConflictException;
import ru.practicum.core.common.exception.NotFoundException;
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
                    "Пользователь с id = %d уже оставил оценку событию с id = %d", raterId, event.getId()));
        }
    }

    @Override
    public void validateAuthorReview(Long raterId, Long authorId, EventShortDto event) {
        if (!eventInterservice.isOrganizer(event.getId(), authorId)) {
            throw new ConflictException("Пользователь с id = " + authorId
                    + " не является организатором события с id = " + event.getId());
        }

        validateDateTime(event.getEventDate());
        validateParticipation(event.getId(), raterId);

        if (userReviewRepository.existsByParams(raterId, event.getId(), authorId)) {
            throw new ConflictException(String.format(
                    "Пользователь с id = %d уже оценивал организатора с id = %d события с id = %d",
                    raterId, authorId, event.getId()));
        }
    }

    private void validateDateTime(LocalDateTime eventDate) {
        LocalDateTime allowedTime = eventDate.plusHours(HOURS_OFFSET_FOR_REVIEWS_CREATING);
        if (LocalDateTime.now().isBefore(allowedTime)) {
            throw new ConflictException(String.format(
                    "Оставить оценку событию можно только спустя %d часов после начала",
                    HOURS_OFFSET_FOR_REVIEWS_CREATING));
        }
    }

    private void validateParticipation(Long eventId, Long userId) {
        if (!participationInterservice.isConfirmedParticipant(userId, eventId)) {
            throw new ConflictException(String.format(
                    "Пользователь с id = %d не является участником события с id = %d", userId, eventId));
        }
    }
}
