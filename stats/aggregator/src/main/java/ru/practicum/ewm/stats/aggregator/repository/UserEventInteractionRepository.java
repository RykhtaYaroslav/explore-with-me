package ru.practicum.ewm.stats.aggregator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.aggregator.model.UserEventInteractionId;

import java.util.List;

public interface UserEventInteractionRepository
        extends JpaRepository<UserEventInteractionEntity, UserEventInteractionId> {

    /**
     * Возвращает взаимодействия пользователя со всеми мероприятиями, кроме указанного.
     *
     * <p>Spring Data строит запрос по вложенным полям составного ключа:
     * {@code id.userId = userId} и {@code id.eventId != eventId}.
     *
     * @param userId  идентификатор пользователя
     * @param eventId идентификатор мероприятия, исключаемого из результата
     * @return взаимодействия с другими мероприятиями или пустой список, если их нет
     */
    List<UserEventInteractionEntity> findAllByIdUserIdAndIdEventIdNot(long userId, long eventId);
}
