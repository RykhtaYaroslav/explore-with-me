package ru.practicum.stats.client.grpc;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.mapper.UserActionMapper;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc.UserActionControllerBlockingStub;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty("grpc.client.collector.address")
public class CollectorGrpcClient {
    private final UserActionControllerBlockingStub stub;
    private final long deadlineMillis;

    public CollectorGrpcClient(@GrpcClient("collector") UserActionControllerBlockingStub stub,
                               @Value("${stats.client.grpc.collector.deadline-ms:2000}") long deadlineMillis) {
        if (deadlineMillis <= 0) {
            throw new IllegalArgumentException("Collector deadline must be positive");
        }
        this.stub = stub;
        this.deadlineMillis = deadlineMillis;
    }

    /**
     * Sends the action and waits for Collector to acknowledge it.
     *
     * @throws StatsGrpcException if the RPC fails or exceeds its deadline
     */
    public void collectUserAction(UserAction action) {
        try {
            stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                    .collectUserAction(UserActionMapper.toProto(action));
        } catch (StatusRuntimeException exception) {
            throw new StatsGrpcException("CollectUserAction", exception);
        }
    }
}
