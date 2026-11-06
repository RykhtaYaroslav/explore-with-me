package ru.practicum.ewm.stats.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.analyzer.service.AnalyzerIngestionService;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
public class AnalyzerKafkaListeners {
    private final AnalyzerIngestionService ingestionService;

    @KafkaListener(
            topics = "${stats.kafka.topics.user-actions}",
            containerFactory = "userActionKafkaListenerContainerFactory"
    )
    public void onUserAction(UserActionAvro message) {
        ingestionService.recordUserAction(message);
    }

    @KafkaListener(
            topics = "${stats.kafka.topics.event-similarity}",
            containerFactory = "eventSimilarityKafkaListenerContainerFactory"
    )
    public void onEventSimilarity(EventSimilarityAvro message) {
        ingestionService.recordSimilarity(message);
    }
}
