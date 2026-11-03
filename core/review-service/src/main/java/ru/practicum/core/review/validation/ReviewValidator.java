package ru.practicum.core.review.validation;

import ru.practicum.core.event.dto.response.EventShortDto;
import ru.practicum.core.user.dto.UserShortDto;

public interface ReviewValidator {
    UserShortDto findUserOrThrow(Long userId);

    EventShortDto findEventOrThrow(Long eventId);

    void validateEventReview(Long raterId, EventShortDto event);

    void validateAuthorReview(Long raterId, Long authorId, EventShortDto event);
}
