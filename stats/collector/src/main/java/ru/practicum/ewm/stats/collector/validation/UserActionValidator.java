package ru.practicum.ewm.stats.collector.validation;

import ru.practicum.ewm.stats.proto.UserActionProto;

public interface UserActionValidator {
    void validate(UserActionProto request);
}
