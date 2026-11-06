package ru.practicum.ewm.stats.collector.error;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.util.concurrent.CompletionException;

@Component
@Slf4j
/** Converts user action validation and Kafka publishing failures into gRPC errors. */
public class UserActionErrorHandlerImpl implements UserActionErrorHandler {
    /**
     * Reports invalid user action input to the gRPC client.
     *
     * @param exception validation failure describing why the request is invalid
     * @param responseObserver observer used to return the gRPC error
     */
    @Override
    public void handleValidationError(IllegalArgumentException exception, StreamObserver<Empty> responseObserver) {
        responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription(exception.getMessage())
                .withCause(exception)
                .asRuntimeException());
    }

    /**
     * Logs a Kafka publishing failure and reports it to the gRPC client.
     * Completion exceptions are unwrapped before the failure is classified.
     *
     * @param action action that could not be published
     * @param failure failure raised while publishing the action
     * @param responseObserver observer used to return the gRPC error
     */
    @Override
    public void handleKafkaFailure(
            UserActionAvro action,
            Throwable failure,
            StreamObserver<Empty> responseObserver
    ) {
        Throwable cause = unwrap(failure);
        log.error("Failed to publish user action for user {} and event {}",
                action.getUserId(), action.getEventId(), cause);
        Status status = cause instanceof SerializationException ? Status.INTERNAL : Status.UNAVAILABLE;
        responseObserver.onError(status
                .withDescription("Could not publish user action to Kafka")
                .withCause(cause)
                .asRuntimeException());
    }

    /**
     * Unwraps nested completion exceptions to expose the underlying failure.
     *
     * @param failure failure to inspect
     * @return the deepest cause reachable through {@link CompletionException} wrappers,
     *         or the original failure if it is not wrapped
     */
    private Throwable unwrap(Throwable failure) {
        Throwable cause = failure;
        while (cause instanceof CompletionException && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }
}
