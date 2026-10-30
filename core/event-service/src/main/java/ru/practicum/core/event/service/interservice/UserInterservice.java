package ru.practicum.core.event.service.interservice;

import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;

public interface UserInterservice {
    void existsById(Long userId);
    //TODO rename: add OrThrow

    UserShortDto getShortById(Long userId);

    List<UserShortDto> getShortByIds(List<Long> userIds);
}
