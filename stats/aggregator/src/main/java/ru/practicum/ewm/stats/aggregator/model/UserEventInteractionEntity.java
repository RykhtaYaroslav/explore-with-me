package ru.practicum.ewm.stats.aggregator.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "aggregator_user_event_interactions",
        indexes = @Index(name = "idx_aggregator_interactions_event_user", columnList = "event_id,user_id")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEventInteractionEntity {
    @EmbeddedId
    private UserEventInteractionId id;

    private double weight;

    private Instant lastInteractionTime;
}
