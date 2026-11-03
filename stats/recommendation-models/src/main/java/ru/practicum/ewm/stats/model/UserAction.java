package ru.practicum.ewm.stats.model;

import java.time.Instant;

public record UserAction(
        long userId,
        long eventId,
        ActionType actionType,
        Instant timestamp
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long userId;
        private long eventId;
        private ActionType actionType;
        private Instant timestamp;

        private Builder() {
        }

        public Builder userId(long userId) {
            this.userId = userId;
            return this;
        }

        public Builder eventId(long eventId) {
            this.eventId = eventId;
            return this;
        }

        public Builder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public UserAction build() {
            return new UserAction(userId, eventId, actionType, timestamp);
        }
    }
}
