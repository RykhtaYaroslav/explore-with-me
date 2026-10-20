package ru.practicum.main.user.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequest dto);

    UserDto toUserDtoOut(User entity);

    UserShortDto toUserShortDto(User user);
}
