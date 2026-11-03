package ru.practicum.core.review.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.core.common.constants.ServiceNames;
import ru.practicum.core.user.dto.UserShortDto;

import java.util.List;

@FeignClient(name = ServiceNames.USER_SERVICE)
public interface UserFeignClient {
    @GetMapping("/interservice/users/exist/{id}")
    boolean existsById(@PathVariable("id") Long userId);

    @GetMapping("/interservice/users/short/{id}")
    UserShortDto getShortById(@PathVariable("id") Long userId);

    @GetMapping("/interservice/users/short")
    List<UserShortDto> getShortByIds(@RequestParam("ids") List<Long> userIds);
}
