package ru.practicum.core.review.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;
import ru.practicum.stats.client.grpc.StatsGrpcException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewLikeListener {
    private final CollectorGrpcClient collector;

    /** Sends the interaction only once the review transaction has committed. */
    @TransactionalEventListener
    public void sendLike(UserAction action) {
        try {
            collector.collectUserAction(action);
        } catch (StatsGrpcException exception) {
            log.warn("Could not send LIKE for user {} and event {}", action.userId(), action.eventId(), exception);
        }
    }
}
