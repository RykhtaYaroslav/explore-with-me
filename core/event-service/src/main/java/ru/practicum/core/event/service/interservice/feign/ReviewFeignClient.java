package ru.practicum.core.event.service.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "review-service")
public interface ReviewFeignClient {


}
