package ru.practicum.core.review.interservice;

import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;

public interface UserInterservice {
    boolean existsById(Long userId);

    UserShortDto getShortById(Long userId);

    List<UserShortDto> getShortByIds(List<Long> userIds);
}
