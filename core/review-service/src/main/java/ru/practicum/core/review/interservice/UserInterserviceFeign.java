package ru.practicum.core.review.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.review.interservice.feign.UserFeignClient;
import ru.practicum.core.user.dto.UserShortDto;

@Component
@RequiredArgsConstructor
public class UserInterserviceFeign implements UserInterservice {
    private final UserFeignClient userFeignClient;

    @Override
    public boolean existsById(Long userId) {
        return userFeignClient.existsById(userId);
    }

    @Override
    public UserShortDto getShortById(Long userId) {
        return userFeignClient.getShortById(userId);
    }
}
