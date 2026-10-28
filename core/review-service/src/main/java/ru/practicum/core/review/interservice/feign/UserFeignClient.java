package ru.practicum.core.review.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.core.common.dto.user.UserShortDto;

@FeignClient(name = "main-service")
public interface UserFeignClient {
    @GetMapping("/interservice/users/exist/{id}")
    boolean existsById(@PathVariable("id") Long userId);

    @GetMapping("/interservice/users/short/{id}")
    UserShortDto getShortById(@PathVariable("id") Long userId);
    //TODO изменить адрес сервиса на сервис юзеров
}
