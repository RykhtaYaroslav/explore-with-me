package ru.practicum.stats.client.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/** Reports a failed stats RPC and preserves its gRPC status and cause. */
public class StatsGrpcException extends RuntimeException {
    StatsGrpcException(String operation, StatusRuntimeException cause) {
        super(String.format("%s failed with gRPC status %s", operation, cause.getStatus().getCode()), cause);
    }

    public Status.Code getStatusCode() {
        return ((StatusRuntimeException) getCause()).getStatus().getCode();
    }
}
