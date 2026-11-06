package ru.practicum.ewm.stats.aggregator.validation;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

/** Проверяет контракт действия без обращений к БД и другим сервисам. */
@Component
public class UserActionValidatorImpl implements UserActionValidator {
    @Override
    public void validate(UserActionAvro action) {
        if (action == null) {
            throw new IllegalArgumentException("action must be set");
        }
        if (action.getUserId() <= 0) {
            throw new IllegalArgumentException("user_id must be positive");
        }
        if (action.getEventId() <= 0) {
            throw new IllegalArgumentException("event_id must be positive");
        }
        if (action.getActionType() == null) {
            throw new IllegalArgumentException("action_type must be set");
        }
        if (action.getTimestamp() == null) {
            throw new IllegalArgumentException("timestamp must be set");
        }
    }
}
