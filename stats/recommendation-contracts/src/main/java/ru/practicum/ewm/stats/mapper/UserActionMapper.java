package ru.practicum.ewm.stats.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.model.action.ActionType;
import ru.practicum.ewm.stats.model.action.UserAction;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.util.Objects;

public final class UserActionMapper {
    private UserActionMapper() {
    }

    public static UserAction toModel(UserActionAvro source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return UserAction.builder()
                .userId(source.getUserId())
                .eventId(source.getEventId())
                .actionType(toModel(source.getActionType()))
                .timestamp(Objects.requireNonNull(source.getTimestamp(), MapperErrorMessages.TIMESTAMP_MUST_NOT_BE_NULL))
                .build();
    }

    public static UserActionAvro toAvro(UserAction source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return UserActionAvro.newBuilder()
                .setUserId(source.userId())
                .setEventId(source.eventId())
                .setActionType(toAvro(source.actionType()))
                .setTimestamp(MapperTimestamps.toAvroTimestamp(source.timestamp()))
                .build();
    }

    public static UserAction toModel(UserActionProto source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        if (!source.hasTimestamp()) {
            throw new IllegalArgumentException(MapperErrorMessages.TIMESTAMP_MUST_BE_SET);
        }
        return UserAction.builder()
                .userId(source.getUserId())
                .eventId(source.getEventId())
                .actionType(toModel(source.getActionType()))
                .timestamp(MapperTimestamps.toInstant(source.getTimestamp()))
                .build();
    }

    public static UserActionProto toProto(UserAction source) {
        Objects.requireNonNull(source, MapperErrorMessages.SOURCE_MUST_NOT_BE_NULL);
        return UserActionProto.newBuilder()
                .setUserId(source.userId())
                .setEventId(source.eventId())
                .setActionType(toProto(source.actionType()))
                .setTimestamp(MapperTimestamps.toProtoTimestamp(source.timestamp()))
                .build();
    }

    public static UserActionAvro toAvro(UserActionProto source) {
        return toAvro(toModel(source));
    }

    public static UserActionProto toProto(UserActionAvro source) {
        return toProto(toModel(source));
    }

    private static ActionType toModel(ActionTypeAvro source) {
        Objects.requireNonNull(source, MapperErrorMessages.ACTION_TYPE_MUST_NOT_BE_NULL);
        return switch (source) {
            case VIEW -> ActionType.VIEW;
            case REGISTER -> ActionType.REGISTER;
            case LIKE -> ActionType.LIKE;
        };
    }

    private static ActionType toModel(ActionTypeProto source) {
        return switch (source) {
            case ACTION_VIEW -> ActionType.VIEW;
            case ACTION_REGISTER -> ActionType.REGISTER;
            case ACTION_LIKE -> ActionType.LIKE;
            case UNRECOGNIZED -> throw new IllegalArgumentException(
                    MapperErrorMessages.UNRECOGNIZED_PROTOBUF_ACTION_TYPE.formatted(source.getNumber())
            );
        };
    }

    private static ActionTypeAvro toAvro(ActionType source) {
        Objects.requireNonNull(source, MapperErrorMessages.ACTION_TYPE_MUST_NOT_BE_NULL);
        return switch (source) {
            case VIEW -> ActionTypeAvro.VIEW;
            case REGISTER -> ActionTypeAvro.REGISTER;
            case LIKE -> ActionTypeAvro.LIKE;
        };
    }

    private static ActionTypeProto toProto(ActionType source) {
        Objects.requireNonNull(source, MapperErrorMessages.ACTION_TYPE_MUST_NOT_BE_NULL);
        return switch (source) {
            case VIEW -> ActionTypeProto.ACTION_VIEW;
            case REGISTER -> ActionTypeProto.ACTION_REGISTER;
            case LIKE -> ActionTypeProto.ACTION_LIKE;
        };
    }

}
