package ru.practicum.core.event.service.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserFeignClient {

    @GetMapping("/interservice/users/exist/{id}")
    boolean existsById(@PathVariable Long id);
}
