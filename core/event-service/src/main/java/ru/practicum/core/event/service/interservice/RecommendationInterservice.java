package ru.practicum.core.event.service.interservice;

import java.util.List;
import java.util.Map;

public interface RecommendationInterservice {
    /** Records a personal view. A delivery failure is logged without failing the event read. */
    void recordView(long userId, long eventId);

    /** Returns distinct recommended event IDs in prediction order. */
    List<Long> getRecommendedEventIds(long userId, int size);

    /** Returns total interaction weights keyed by event ID. */
    Map<Long, Double> getInteractionWeights(List<Long> eventIds);
}
