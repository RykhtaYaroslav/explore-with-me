package ru.practicum.core.review.interservice;

import ru.practicum.core.common.dto.user.UserShortDto;

public interface UserInterservice {
    boolean existsById(Long userId);

    UserShortDto getShortById(Long userId);
}
