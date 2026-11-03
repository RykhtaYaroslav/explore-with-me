package ru.practicum.ewm.stats.model;

import java.time.Instant;

public record EventSimilarity(
        long eventA,
        long eventB,
        double score,
        Instant timestamp
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private long eventA;
        private long eventB;
        private double score;
        private Instant timestamp;

        private Builder() {
        }

        public Builder eventA(long eventA) {
            this.eventA = eventA;
            return this;
        }

        public Builder eventB(long eventB) {
            this.eventB = eventB;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public EventSimilarity build() {
            return new EventSimilarity(eventA, eventB, score, timestamp);
        }
    }
}
