package ru.practicum.main.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.request.dto.ConfirmedRequestsCount;
import ru.practicum.main.request.model.ParticipationRequest;
import ru.practicum.main.request.model.RequestStatus;

import java.util.List;

public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("""
            SELECT new ru.practicum.main.request.dto.ConfirmedRequestsCount(r.event.id, COUNT(r.id))
            FROM ParticipationRequest r
            WHERE r.event.id IN :eventIds AND  r.status = :status
            GROUP BY r.event.id
            """)
    List<ConfirmedRequestsCount> countRequestsCountByEventIds(@Param("eventIds") List<Long> eventIds,
                                                              @Param("status") RequestStatus status);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    boolean existsByRequesterIdAndEventIdAndStatus(Long requesterId, Long eventId, RequestStatus status);
}
