package ru.practicum.core.event.dto;

/**
 * Repository projection containing an event ID and its average review score.
 * Despite the class name, {@code rating} is an average value, not a review count.
 *
 * @param eventId ID of the event whose reviews were aggregated
 * @param rating average score of the event's reviews
 */
public record EventRatingCount(Long eventId, Double rating) {
}
