package ru.practicum.ewm.stats.collector.validation;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.UserActionProto;

@Component
public class UserActionValidatorImpl implements UserActionValidator {
    @Override
    public void validate(UserActionProto request) {
        if (request.getUserId() <= 0) {
            throw new IllegalArgumentException("user_id must be positive");
        }
        if (request.getEventId() <= 0) {
            throw new IllegalArgumentException("event_id must be positive");
        }
        if (!request.hasTimestamp()) {
            throw new IllegalArgumentException("timestamp must be set");
        }
        if (request.getTimestamp().getNanos() < 0 || request.getTimestamp().getNanos() >= 1_000_000_000) {
            throw new IllegalArgumentException("timestamp nanos must be between 0 and 999999999");
        }
    }
}
