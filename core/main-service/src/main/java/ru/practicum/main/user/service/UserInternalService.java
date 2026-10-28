package ru.practicum.main.user.service;

import ru.practicum.core.common.dto.user.UserShortDto;

public interface UserInternalService {
    boolean existsById(Long id);

    UserShortDto getShortById(Long id);
}
