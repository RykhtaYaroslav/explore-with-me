package ru.practicum.ewm.stats.mapper;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.model.EventSimilarity;

import java.util.Objects;

public final class EventSimilarityMapper {
    private EventSimilarityMapper() {
    }

    public static EventSimilarity toModel(EventSimilarityAvro source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return EventSimilarity.builder()
                .eventA(source.getEventA())
                .eventB(source.getEventB())
                .score(source.getScore())
                .timestamp(Objects.requireNonNull(source.getTimestamp(), MapperErrorMessages.TIMESTAMP_MUST_NOT_BE_NULL))
                .build();
    }

    public static EventSimilarityAvro toAvro(EventSimilarity source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return EventSimilarityAvro.newBuilder()
                .setEventA(source.eventA())
                .setEventB(source.eventB())
                .setScore(source.score())
                .setTimestamp(MapperTimestamps.toAvroTimestamp(source.timestamp()))
                .build();
    }
}
