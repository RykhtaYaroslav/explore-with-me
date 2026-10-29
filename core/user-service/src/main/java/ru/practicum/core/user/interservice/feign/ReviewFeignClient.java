package ru.practicum.core.user.interservice.feign;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "review-service")
public interface ReviewFeignClient {

}
