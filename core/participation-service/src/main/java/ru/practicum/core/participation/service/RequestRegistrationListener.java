package ru.practicum.core.participation.service;

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
public class RequestRegistrationListener {
    private final CollectorGrpcClient collector;

    /** Sends REGISTER after the participation request has committed. */
    @TransactionalEventListener
    public void sendRegistration(UserAction action) {
        try {
            collector.collectUserAction(action);
        } catch (StatsGrpcException exception) {
            log.warn("Could not send REGISTER for user {} and event {}", action.userId(), action.eventId(), exception);
        }
    }
}
