package ru.practicum.core.review.interservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.core.review.interservice.feign.RequestFeignClient;

@Component
@RequiredArgsConstructor
public class ParticipationInterserviceFeign implements ParticipationInterservice {
    private final RequestFeignClient requestFeignClient;

    @Override
    public boolean isConfirmedParticipant(Long userId, Long eventId) {
        return requestFeignClient.isConfirmedParticipant(userId, eventId);
    }
}
