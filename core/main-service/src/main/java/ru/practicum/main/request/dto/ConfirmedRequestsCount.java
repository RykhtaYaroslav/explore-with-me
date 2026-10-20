package ru.practicum.main.request.dto;


/**
 * Projection DTO used to encapsulate the total count of participation requests associated with a specific event.
 * <p>
 * Primarily used in JPQL aggregate queries with a {@code GROUP BY} clause to retrieve participation request counts
 * for multiple events in a single database round-trip, preventing the N+1 query problem.
 * </p>
 *
 * @param eventId the unique identifier of the target event
 * @param count   the total number of participation requests for the specified event and request status
 */
public record ConfirmedRequestsCount(Long eventId, Long count) {
}
// DTO for extracting confirmed requests from RequestRepository for eventServiceImpl