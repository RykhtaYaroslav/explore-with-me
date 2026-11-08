package ru.practicum.ewm.stats.mapper;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.util.Objects;

final class MapperTimestamps {
    private MapperTimestamps() {
    }

    static Instant toAvroTimestamp(Instant source) {
        return Instant.ofEpochMilli(requireTimestamp(source).toEpochMilli());
    }

    static Instant toInstant(Timestamp source) {
        return Instant.ofEpochSecond(source.getSeconds(), source.getNanos());
    }

    static Timestamp toProtoTimestamp(Instant source) {
        Instant timestamp = requireTimestamp(source);
        return Timestamp.newBuilder()
                .setSeconds(timestamp.getEpochSecond())
                .setNanos(timestamp.getNano())
                .build();
    }

    static Instant requireTimestamp(Instant source) {
        return Objects.requireNonNull(source, MapperErrorMessages.TIMESTAMP_MUST_NOT_BE_NULL);
    }
}
