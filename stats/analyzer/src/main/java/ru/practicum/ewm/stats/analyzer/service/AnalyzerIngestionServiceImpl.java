package ru.practicum.ewm.stats.analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityEntity;
import ru.practicum.ewm.stats.analyzer.model.EventSimilarityId;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionId;
import ru.practicum.ewm.stats.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.analyzer.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.mapper.RecommendationMapper;
import ru.practicum.ewm.stats.mapper.UserActionMapper;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.model.similarity.EventSimilarity;

/**
 * Stores maximum user-event weights and the latest calculated event-pair scores.
 */
@Service
@RequiredArgsConstructor
public class AnalyzerIngestionServiceImpl implements AnalyzerIngestionService {
    private final UserEventInteractionRepository interactionRepository;
    private final EventSimilarityRepository similarityRepository;

    /**
     * Stores the maximum action weight, latest interaction time and whether the user viewed the event.
     *
     * @param message action received from Collector's Kafka topic
     * @throws IllegalArgumentException if user or event id is not positive
     */
    @Transactional
    @Override
    public void recordUserAction(UserActionAvro message) {
        if (message.getUserId() <= 0 || message.getEventId() <= 0) {
            throw new IllegalArgumentException("user_id and event_id must be positive");
        }
        UserAction action = UserActionMapper.toModel(message);
        UserEventInteractionId id = new UserEventInteractionId(action.userId(), action.eventId());
        UserEventInteractionEntity interaction = interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteractionEntity(id, 0.0, action.timestamp(), false));
        interaction.setWeight(Math.max(interaction.getWeight(), action.actionType().getWeight()));
        interaction.setViewed(interaction.isViewed() || action.actionType() == ActionType.VIEW);
        if (action.timestamp().isAfter(interaction.getLastInteractionTime())) {
            interaction.setLastInteractionTime(action.timestamp());
        }
        interactionRepository.save(interaction);
    }

    /**
     * Replaces the saved score for a canonical pair with the latest score from Aggregator.
     *
     * @param message similarity received from Aggregator's Kafka topic
     */
    @Transactional
    @Override
    public void recordSimilarity(EventSimilarityAvro message) {
        EventSimilarity similarity = RecommendationMapper.toModel(message);
        if (similarity.eventA() >= similarity.eventB()) {
            throw new IllegalArgumentException("eventA must be less than eventB");
        }
        if (!Double.isFinite(similarity.score()) || similarity.score() < 0.0 || similarity.score() > 1.0) {
            throw new IllegalArgumentException("similarity score must be between 0 and 1");
        }

        EventSimilarityId id = new EventSimilarityId(similarity.eventA(), similarity.eventB());
        similarityRepository.save(new EventSimilarityEntity(id, similarity.score(), similarity.timestamp()));
    }
}
