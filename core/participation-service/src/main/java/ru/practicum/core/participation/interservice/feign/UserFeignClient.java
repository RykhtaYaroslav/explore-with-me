package ru.practicum.core.participation.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.core.common.constants.ServiceNames;

@FeignClient(name = ServiceNames.USER_SERVICE)
public interface UserFeignClient {
    @GetMapping("/interservice/users/exist/{id}")
    boolean existsById(@PathVariable Long id);
}
