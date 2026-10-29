package ru.practicum.core.user.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;
import ru.practicum.core.common.constants.ServiceNames;

@FeignClient(name = ServiceNames.REVIEW_SERVICE)
public interface ReviewFeignClient {
// TODO
}
