package ru.practicum.ewm.stats.aggregator.validation;

import ru.practicum.ewm.stats.avro.UserActionAvro;

/**
 * Проверяет входные действия перед обновлением агрегатов.
 */
public interface UserActionValidator {
    /**
     * Проверяет наличие сообщения, положительные идентификаторы, тип и время действия.
     *
     * @param action входное сообщение из Kafka
     * @throws IllegalArgumentException если сообщение или обязательные поля некорректны
     */
    void validate(UserActionAvro action);
}
