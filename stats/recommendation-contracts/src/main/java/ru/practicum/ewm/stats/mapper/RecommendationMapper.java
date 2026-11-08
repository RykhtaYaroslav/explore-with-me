package ru.practicum.ewm.stats.mapper;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.request.SimilarEventsRequest;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.ewm.stats.model.similarity.EventSimilarity;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.util.Objects;

public final class RecommendationMapper {
    private RecommendationMapper() {
    }

    public static UserPredictionsRequest toModel(UserPredictionsRequestProto source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return UserPredictionsRequest.builder()
                .userId(source.getUserId())
                .maxResults(source.getMaxResults())
                .build();
    }

    public static UserPredictionsRequestProto toProto(UserPredictionsRequest source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return UserPredictionsRequestProto.newBuilder()
                .setUserId(source.userId())
                .setMaxResults(source.maxResults())
                .build();
    }

    public static SimilarEventsRequest toModel(SimilarEventsRequestProto source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return SimilarEventsRequest.builder()
                .eventId(source.getEventId())
                .userId(source.getUserId())
                .maxResults(source.getMaxResults())
                .build();
    }

    public static SimilarEventsRequestProto toProto(SimilarEventsRequest source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return SimilarEventsRequestProto.newBuilder()
                .setEventId(source.eventId())
                .setUserId(source.userId())
                .setMaxResults(source.maxResults())
                .build();
    }

    public static InteractionsCountRequest toModel(InteractionsCountRequestProto source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return InteractionsCountRequest.builder()
                .eventIds(source.getEventIdList())
                .build();
    }

    public static InteractionsCountRequestProto toProto(InteractionsCountRequest source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return InteractionsCountRequestProto.newBuilder()
                .addAllEventId(source.eventIds())
                .build();
    }

    public static RecommendedEvent toModel(RecommendedEventProto source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return RecommendedEvent.builder()
                .eventId(source.getEventId())
                .score(source.getScore())
                .build();
    }

    public static RecommendedEventProto toProto(RecommendedEvent source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return RecommendedEventProto.newBuilder()
                .setEventId(source.eventId())
                .setScore(source.score())
                .build();
    }

    public static EventSimilarity toModel(EventSimilarityAvro source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return EventSimilarity.builder()
                .eventA(source.getEventA())
                .eventB(source.getEventB())
                .score(source.getScore())
                .timestamp(Objects.requireNonNull(
                        source.getTimestamp(),
                        MapperErrorMessages.TIMESTAMP_MUST_NOT_BE_NULL
                ))
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
