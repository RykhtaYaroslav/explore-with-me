package ru.practicum.stats.client.grpc;

import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.mapper.RecommendationMapper;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.request.SimilarEventsRequest;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.dashboard.RecommendationsControllerGrpc.RecommendationsControllerBlockingStub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Reads Analyzer streams into lists of shared Java models in server order.
 * A failed stream throws {@link StatsGrpcException}, including after a partial response.
 */
@Component
@ConditionalOnProperty("grpc.client.analyzer.address")
public class AnalyzerGrpcClient {
    private final RecommendationsControllerBlockingStub stub;
    private final long deadlineMillis;

    public AnalyzerGrpcClient(@GrpcClient("analyzer") RecommendationsControllerBlockingStub stub,
                              @Value("${stats.client.grpc.analyzer.deadline-ms:2000}") long deadlineMillis) {
        if (deadlineMillis <= 0) {
            throw new IllegalArgumentException("Analyzer deadline must be positive");
        }
        this.stub = stub;
        this.deadlineMillis = deadlineMillis;
    }

    /**
     * Returns events scored by their predicted interaction weight for the user.
     */
    public List<RecommendedEvent> getRecommendationsForUser(UserPredictionsRequest request) {
        return read("GetRecommendationsForUser", () -> stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                .getRecommendationsForUser(RecommendationMapper.toProto(request)));
    }

    /**
     * Returns events scored by their similarity to the requested event.
     */
    public List<RecommendedEvent> getSimilarEvents(SimilarEventsRequest request) {
        return read("GetSimilarEvents", () -> stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                .getSimilarEvents(RecommendationMapper.toProto(request)));
    }

    /**
     * Returns the sum of users' maximum action weights for each requested event.
     */
    public List<RecommendedEvent> getInteractionsCount(InteractionsCountRequest request) {
        return read("GetInteractionsCount", () -> stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                .getInteractionsCount(RecommendationMapper.toProto(request)));
    }

    /** Returns unique viewer counts keyed by event ID. Missing IDs have zero views. */
    public Map<Long, Long> getViewCounts(InteractionsCountRequest request) {
        try {
            return stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                    .getViewCounts(RecommendationMapper.toProto(request))
                    .getViewsByEventMap();
        } catch (StatusRuntimeException exception) {
            throw new StatsGrpcException("GetViewCounts", exception);
        }
    }

    private List<RecommendedEvent> read(String operation, Supplier<Iterator<RecommendedEventProto>> call) {
        List<RecommendedEvent> events = new ArrayList<>();
        try {
            call.get().forEachRemaining(event -> events.add(RecommendationMapper.toModel(event)));
        } catch (StatusRuntimeException exception) {
            throw new StatsGrpcException(operation, exception);
        }
        return List.copyOf(events);
    }
}
