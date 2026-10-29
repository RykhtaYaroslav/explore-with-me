package ru.practicum.core.user.service;

import ru.practicum.core.user.dto.NewUserRequest;
import ru.practicum.core.user.dto.UserDto;

import java.util.List;

public interface UserService {
    List<UserDto> getUsers(List<Long> ids, int from, int size);

    UserDto createUser(NewUserRequest user);

    void deleteUser(long userId);
}
