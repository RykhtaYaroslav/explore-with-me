package ru.practicum.core.event.service.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.core.user.dto.UserShortDto;

@FeignClient(name = "main-service")
public interface UserFeignClient {

    @GetMapping("/interservice/users/exist/{id}")
    boolean existsById(@PathVariable Long id);

    @GetMapping("/interservice/users/short/{id}")
    UserShortDto getShortById(@PathVariable Long id);
}
