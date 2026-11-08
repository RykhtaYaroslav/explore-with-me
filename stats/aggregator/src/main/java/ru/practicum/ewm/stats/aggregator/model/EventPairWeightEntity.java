package ru.practicum.ewm.stats.aggregator.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Sum of per-user minimum weights shared by a pair of events.
 */
@Entity
@Table(name = "aggregator_event_pair_weights")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EventPairWeightEntity {
    @EmbeddedId
    private EventPairWeightId id;

    private double sharedWeight;
}
