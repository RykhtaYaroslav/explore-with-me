package ru.practicum.ewm.stats.analyzer.service;

import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface AnalyzerIngestionService {
    void recordUserAction(UserActionAvro message);

    void recordSimilarity(EventSimilarityAvro message);
}
