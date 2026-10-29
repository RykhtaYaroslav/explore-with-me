package ru.practicum.core.user.service;

import ru.practicum.core.user.dto.UserShortDto;

public interface UserInternalService {
    boolean existsById(Long id);

    UserShortDto getShortById(Long id);
}
