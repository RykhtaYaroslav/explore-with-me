package ru.practicum.ewm.stats.collector.error;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import ru.practicum.ewm.stats.avro.UserActionAvro;

/**
 * Handles validation and Kafka publishing failures for user action requests.
 */
public interface UserActionErrorHandler {
    /**
     * Reports a request validation failure to the gRPC client.
     *
     * @param exception        validation failure
     * @param responseObserver observer used to send the error response
     */
    void handleValidationError(IllegalArgumentException exception, StreamObserver<Empty> responseObserver);

    /**
     * Reports a failure to publish an action to Kafka.
     *
     * @param action           action that could not be published
     * @param failure          publishing failure
     * @param responseObserver observer used to send the error response
     */
    void handleKafkaFailure(UserActionAvro action, Throwable failure, StreamObserver<Empty> responseObserver);
}
