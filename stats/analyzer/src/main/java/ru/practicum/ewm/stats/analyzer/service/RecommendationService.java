package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.RecommendationProperties;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository.EventScore;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.model.request.InteractionsCountRequest;
import ru.practicum.ewm.stats.model.request.SimilarEventsRequest;
import ru.practicum.ewm.stats.model.request.UserPredictionsRequest;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {
    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;
    private final RecommendationProperties properties;

    public List<RecommendedEvent> getSimilarEvents(SimilarEventsRequest request) {
        if (request.maxResults() <= 0) {
            return List.of();
        }
        return toRecommendedEvents(similarityRepository.findUnseenCandidates(
                request.userId(), List.of(request.eventId()), request.maxResults()));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<RecommendedEvent> getRecommendationsForUser(UserPredictionsRequest request) {
        if (request.maxResults() <= 0) {
            return List.of();
        }
        int recentLimit = properties.recentEvents();
        List<Long> recentEventIds = interactionRepository.findRecentEventIds(
                request.userId(), PageRequest.of(0, recentLimit));
        if (recentEventIds.isEmpty()) {
            return List.of();
        }
        List<Long> candidates = similarityRepository.findUnseenCandidates(request.userId(), recentEventIds, recentLimit)
                .stream()
                .map(EventScore::getEventId)
                .toList();
        if (candidates.isEmpty()) {
            return List.of();
        }
        return toRecommendedEvents(similarityRepository.predictScores(
                request.userId(), candidates, properties.neighbors(), request.maxResults()));
    }

    public List<RecommendedEvent> getInteractionsCount(InteractionsCountRequest request) {
        if (request.eventIds().isEmpty()) {
            return List.of();
        }
        Map<Long, Double> totals = interactionRepository.sumWeightsByEventIds(request.eventIds()).stream()
                .collect(Collectors.toMap(RecommendedEvent::eventId, RecommendedEvent::score));
        return request.eventIds().stream()
                .map(eventId -> new RecommendedEvent(eventId, totals.getOrDefault(eventId, 0.0)))
                .toList();
    }

    private List<RecommendedEvent> toRecommendedEvents(List<EventScore> scores) {
        return scores.stream()
                .map(score -> new RecommendedEvent(score.getEventId(), score.getScore()))
                .toList();
    }
}
