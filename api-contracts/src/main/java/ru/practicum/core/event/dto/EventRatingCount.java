package ru.practicum.core.event.dto;

/**
 * Repository projection containing an eventId ID and its average review score.
 * Despite the class name, {@code rating} is an average value, not a review count.
 *
 * @param eventId ID of the eventId whose reviews were aggregated
 * @param rating average score of the eventId's reviews
 */
public record EventRatingCount(Long eventId, Double rating) {
}
