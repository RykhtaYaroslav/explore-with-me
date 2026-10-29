package ru.practicum.core.participation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.core.common.enums.RequestStatus;
import ru.practicum.core.participation.model.ParticipationRequest;

import java.util.List;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);
}
