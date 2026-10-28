package ru.practicum.core.review.validation;

import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.event.dto.response.EventShortDto;

public interface ReviewValidator {
    UserShortDto findUserOrThrow(Long userId);

    EventShortDto findEventOrThrow(Long eventId);

    void validateEventReview(Long raterId, EventShortDto event);

    void validateAuthorReview(Long raterId, Long authorId, EventShortDto event);
}
