package ru.practicum.ewm.stats.collector;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.error.UserActionErrorHandler;
import ru.practicum.ewm.stats.collector.validation.UserActionValidator;
import ru.practicum.ewm.stats.mapper.UserActionMapper;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.collector.UserActionControllerGrpc;

/**
 * Validates incoming user actions and publishes them to Kafka through gRPC.
 */
@GrpcService
@RequiredArgsConstructor
public class UserActionGrpcService extends UserActionControllerGrpc.UserActionControllerImplBase {
    private final KafkaTemplate<String, SpecificRecord> kafkaTemplate;
    private final UserActionValidator userActionValidator;
    private final UserActionErrorHandler userActionErrorHandler;

    /**
     * Validates and converts a user action request, then publishes it to Kafka.
     * On success, sends an empty response; validation and publishing failures are
     * delegated to the configured error handler.
     *
     * @param request          user action received from the gRPC client
     * @param responseObserver observer used to return completion or an error
     */
    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        final UserActionAvro action;
        try {
            userActionValidator.validate(request);
            action = UserActionMapper.toAvro(request);
        } catch (IllegalArgumentException exception) {
            userActionErrorHandler.handleValidationError(exception, responseObserver);
            return;
        }

        try {
            kafkaTemplate.sendDefault(Long.toString(action.getUserId()), action)
                    .whenComplete((result, failure) -> {
                        if (failure != null) {
                            userActionErrorHandler.handleKafkaFailure(action, failure, responseObserver);
                            return;
                        }

                        responseObserver.onNext(Empty.getDefaultInstance());
                        responseObserver.onCompleted();
                    });
        } catch (RuntimeException exception) {
            userActionErrorHandler.handleKafkaFailure(action, exception, responseObserver);
        }
    }
}



