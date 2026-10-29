package ru.practicum.core.user.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.core.common.dto.user.UserShortDto;
import ru.practicum.core.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequest dto);

    UserDto toUserDtoOut(User entity);

    UserShortDto toUserShortDto(User user);
}
