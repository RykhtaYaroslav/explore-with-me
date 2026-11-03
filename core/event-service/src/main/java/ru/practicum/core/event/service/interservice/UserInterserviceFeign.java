package ru.practicum.core.event.service.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.common.exception.NotFoundException;
import ru.practicum.core.event.service.interservice.feign.UserFeignClient;
import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;

@RequiredArgsConstructor
@Component
public class UserInterserviceFeign implements UserInterservice {
    private final UserFeignClient userFeignClient;

    @Override
    public void checkExistsByIdOrThrow(Long userId) {
        if (!userFeignClient.existsById(userId)) {
            throw new NotFoundException(String.format("User with id = %d was not found", userId));
        }
    }

    @Override
    public UserShortDto getShortById(Long userId) {
        checkExistsByIdOrThrow(userId);
        return userFeignClient.getShortById(userId);
    }

    @Override
    public List<UserShortDto> getShortByIds(List<Long> userIds) {
        return userFeignClient.getShortByIds(userIds);
    }
}
