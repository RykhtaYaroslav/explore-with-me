package ru.practicum.core.review.interservice;

import ru.practicum.core.user.dto.UserShortDto;

public interface UserInterservice {
    boolean existsById(Long userId);

    UserShortDto getShortById(Long userId);
}
