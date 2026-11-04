package ru.practicum.ewm.stats.mapper;

final class MapperErrorMessages {
    static final String SOURCE_MUST_NOT_BE_NULL = "source must not be null";
    static final String TIMESTAMP_MUST_NOT_BE_NULL = "timestamp must not be null";
    static final String ACTION_TYPE_MUST_NOT_BE_NULL = "actionType must not be null";
    static final String TIMESTAMP_MUST_BE_SET = "timestamp must be set";
    static final String UNRECOGNIZED_PROTOBUF_ACTION_TYPE = "Unrecognized Protobuf action type: %d";

    private MapperErrorMessages() {
    }
}
