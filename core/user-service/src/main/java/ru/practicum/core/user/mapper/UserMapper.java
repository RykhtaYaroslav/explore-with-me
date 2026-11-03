package ru.practicum.core.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.user.dto.NewUserRequest;
import ru.practicum.core.user.dto.UserDto;
import ru.practicum.core.user.dto.UserShortDto;
import ru.practicum.core.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequest dto);

    UserDto toUserDtoOut(User entity, Double rating);

    UserShortDto toUserShortDto(User user, Double rating);
}
