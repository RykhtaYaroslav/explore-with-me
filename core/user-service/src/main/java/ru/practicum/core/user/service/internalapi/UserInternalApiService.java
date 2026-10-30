package ru.practicum.core.user.service.internalapi;

import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;

public interface UserInternalApiService {
    boolean existsById(Long id);

    UserShortDto getShortById(Long id);

    List<UserShortDto> getShortByIds(List<Long> ids);
}
