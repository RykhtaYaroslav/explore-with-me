package ru.practicum.core.event.service.interservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;
import ru.practicum.stats.client.grpc.AnalyzerGrpcClient;
import ru.practicum.stats.client.grpc.CollectorGrpcClient;
import ru.practicum.stats.client.grpc.StatsGrpcException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendationInterserviceGrpc implements RecommendationInterservice {
    private final CollectorGrpcClient collector;
    private final AnalyzerGrpcClient analyzer;

    @Override
    public void recordView(long userId, long eventId) {
        try {
            collector.collectUserAction(new UserAction(userId, eventId, ActionType.VIEW, Instant.now()));
        } catch (StatsGrpcException exception) {
            log.warn("Could not send VIEW for user {} and event {}", userId, eventId, exception);
        }
    }

    @Override
    public List<Long> getRecommendedEventIds(long userId, int size) {
        return analyzer.getRecommendationsForUser(new UserPredictionsRequest(userId, size)).stream()
                .map(RecommendedEvent::eventId)
                .distinct()
                .toList();
    }

    @Override
    public Map<Long, Double> getInteractionWeights(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return analyzer.getInteractionsCount(new InteractionsCountRequest(eventIds.stream().distinct().toList()))
                .stream()
                .collect(Collectors.toMap(RecommendedEvent::eventId, RecommendedEvent::score));
    }

    @Override
    public Map<Long, Long> getViewCounts(List<Long> eventIds) {
        return analyzer.getViewCounts(new InteractionsCountRequest(eventIds.stream().distinct().toList()));
    }
}
