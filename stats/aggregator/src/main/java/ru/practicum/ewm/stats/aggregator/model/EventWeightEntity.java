package ru.practicum.ewm.stats.aggregator.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Sum of maximum user interaction weights for one event. */
@Entity
@Table(name = "aggregator_event_weights")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EventWeightEntity {
    @Id
    @Column(name = "event_id")
    private long eventId;

    @Column(nullable = false)
    private double totalWeight;
}
