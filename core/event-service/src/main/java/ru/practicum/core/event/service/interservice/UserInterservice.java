package ru.practicum.core.event.service.interservice;

import ru.practicum.core.common.dto.user.UserShortDto;

public interface UserInterservice {
    void existsById(Long userId);

    UserShortDto getShortById(Long userId);
}
