package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserActionKafkaListener {

    private final UserActionSimilarityProcessor processor;

    @KafkaListener(
            topics = "${stats.kafka.topics.user-actions}",
            containerFactory = "kafkaListenerContainerFactory",
            autoStartup = "false"
    )
    public void onUserAction(UserActionAvro action) {
        processor.process(action);
        log.debug("Processed user action for user {} and event {}", action.getUserId(), action.getEventId());
    }
}
