package ru.practicum.ewm.stats.analyzer.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Latest calculated similarity score for a canonical pair of events.
 */
@Entity
@Table(name = "event_similarities")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EventSimilarityEntity {
    @EmbeddedId
    private EventSimilarityId id;

    private double score;

    private Instant timestamp;
}
