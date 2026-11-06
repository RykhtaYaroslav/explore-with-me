package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.EventWeightRepository;

/** Backfills aggregate rows from existing interactions before starting Kafka consumption. */
@Component
@RequiredArgsConstructor
public class AggregateWeightsInitializer implements ApplicationRunner {
    private final EventWeightRepository eventWeightRepository;
    private final EventPairWeightRepository eventPairWeightRepository;
    private final KafkaListenerEndpointRegistry listenerRegistry;

    @Override
    public void run(ApplicationArguments args) {
        eventWeightRepository.backfillMissingWeights();
        eventPairWeightRepository.backfillMissingPairs();
        listenerRegistry.start();
    }
}
