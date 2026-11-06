package ru.practicum.ewm.stats.aggregator;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionId;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.EventPairWeightRepository.SimilarityAggregate;
import ru.practicum.ewm.stats.aggregator.repository.EventWeightRepository;
import ru.practicum.ewm.stats.aggregator.repository.UserEventInteractionRepository;
import ru.practicum.ewm.stats.aggregator.validation.UserActionValidator;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.model.action.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Инкрементально обновляет сходство мероприятий по действиям пользователей.
 *
 * <p>Для пары пользователь–мероприятие хранит максимальный вес действия. При его росте
 * увеличивает общий вес мероприятия и суммы минимальных весов для его пар с другими
 * мероприятиями пользователя. Затем пересчитывает все пары изменённого мероприятия:
 * рост общего веса меняет знаменатель даже у пар с неизменившейся общей суммой.
 *
 * <p>Использует только репозитории Aggregator. Изменения БД выполняются в одной транзакции;
 * подтверждения публикаций в Kafka ожидаются до выхода из метода. Это ожидание не делает
 * транзакцию БД и публикации в Kafka атомарными: уже отправленные сообщения не откатываются.
 */
@Service
@RequiredArgsConstructor
public class UserActionSimilarityProcessorImpl implements UserActionSimilarityProcessor {
    private final EventSimilarityPublisher publisher;
    private final UserEventInteractionRepository interactionRepository;
    private final EventWeightRepository eventWeightRepository;
    private final EventPairWeightRepository eventPairWeightRepository;
    private final UserActionValidator userActionValidator;

    /**
     * Проверяет сообщение, обновляет максимальный вес взаимодействия и публикует сходство.
     *
     * <p>Повторное или менее весомое действие может обновить только время взаимодействия,
     * если оно новее сохранённого. Суммы и сходство в этом случае не пересчитываются.
     * Исключения не перехватываются, чтобы вызывающий Kafka listener мог обработать сбой.
     *
     * @param action действие пользователя с положительными идентификаторами, типом и временем
     * @throws IllegalArgumentException если сообщение не прошло валидацию
     * @throws RuntimeException если сохранение или публикация завершились ошибкой;
     *                          изменения БД откатываются
     */
    @Override
    @Transactional
    public void process(UserActionAvro action) {
        userActionValidator.validate(action);

        double newWeight = toActionType(action).getWeight();

        UserEventInteractionId id = new UserEventInteractionId(action.getUserId(), action.getEventId());
        UserEventInteractionEntity interaction = interactionRepository.findById(id)
                .orElseGet(() -> new UserEventInteractionEntity(id, 0.0, action.getTimestamp()));
        double oldWeight = interaction.getWeight();

        if (newWeight <= oldWeight) {
            if (action.getTimestamp().isAfter(interaction.getLastInteractionTime())) {
                interaction.setLastInteractionTime(action.getTimestamp());
                interactionRepository.save(interaction);
            }
            return;
        }

        double delta = newWeight - oldWeight;
        interaction.setWeight(newWeight);
        if (action.getTimestamp().isAfter(interaction.getLastInteractionTime())) {
            interaction.setLastInteractionTime(action.getTimestamp());
        }
        interactionRepository.save(interaction);
        eventWeightRepository.addWeight(action.getEventId(), delta);

        eventPairWeightRepository.updateSharedWeights(action.getUserId(), action.getEventId(), oldWeight, newWeight);
        publishChangedSimilarities(action);
    }

    /**
     * Одним запросом получает суммы для всех пар мероприятия и публикует их сходство.
     *
     * <p>Сходство равно общей сумме минимальных весов, делённой на квадратный корень
     * произведения общих весов мероприятий. Пары с нулевым знаменателем пропускаются,
     * результат ограничивается сверху единицей. Метод ожидает все отправленные сообщения.
     *
     * @param action действие, задающее изменённое мероприятие и время публикуемых результатов
     * @throws RuntimeException если чтение сумм или отправка в Kafka завершились ошибкой
     */
    private void publishChangedSimilarities(UserActionAvro action) {
        List<CompletableFuture<?>> sends = new ArrayList<>();
        for (SimilarityAggregate aggregate : eventPairWeightRepository
                .findSimilaritiesForEvent(action.getEventId())) {
            double denominator = Math.sqrt(aggregate.getEventAWeight() * aggregate.getEventBWeight());
            if (denominator == 0.0) {
                continue;
            }
            double score = Math.min(1.0, aggregate.getSharedWeight() / denominator);
            EventSimilarityAvro similarity = EventSimilarityAvro.newBuilder()
                    .setEventA(aggregate.getEventA())
                    .setEventB(aggregate.getEventB())
                    .setScore(score)
                    .setTimestamp(action.getTimestamp())
                    .build();
            sends.add(publisher.publish(similarity));
        }
        CompletableFuture.allOf(sends.toArray(CompletableFuture[]::new)).join();
    }

    /**
     * Преобразует Avro-тип проверенного действия в общую модель с весами действий.
     *
     * @param action действие, уже прошедшее валидацию
     * @return соответствующий тип действия из общей модели
     */
    private ActionType toActionType(UserActionAvro action) {
        return switch (action.getActionType()) {
            case VIEW -> ActionType.VIEW;
            case REGISTER -> ActionType.REGISTER;
            case LIKE -> ActionType.LIKE;
        };
    }
}
