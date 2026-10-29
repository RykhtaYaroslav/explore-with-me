package ru.practicum.core.event.service.interservice;

import ru.practicum.core.user.dto.UserShortDto;

public interface UserInterservice {
    void existsById(Long userId);
    //TODO rename: add OrThrow

    UserShortDto getShortById(Long userId);
}
