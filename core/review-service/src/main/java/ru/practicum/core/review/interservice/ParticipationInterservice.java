package ru.practicum.core.review.interservice;

public interface ParticipationInterservice {
    boolean isConfirmedParticipant(Long userId, Long eventId);
}
