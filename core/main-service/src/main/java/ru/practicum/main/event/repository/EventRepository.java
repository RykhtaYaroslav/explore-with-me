package ru.practicum.main.event.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.event.model.EventState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    @SuppressWarnings("all")
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.initiator.id = :userId
            ORDER BY e.id ASC
            """)
    List<Event> findByInitiatorId(@Param("userId") Long userId,
                                  Pageable pageable);

    Optional<Event> findByIdAndInitiatorId(Long id, Long initiatorId);

    @Query("select count(e) from Event e where e.category.id = :catId")
    long countEventBySelectCategory(@Param("catId") long catId);

    boolean existsByCategoryId(Long categoryId);

    @SuppressWarnings("all")
    @Query("""
            SELECT e
            FROM Event e
            WHERE (:users IS NULL OR e.initiator.id IN :users)
              AND (:states IS NULL OR e.state IN :states)
              AND (CAST(:text AS string) IS NULL OR (LOWER(e.annotation) LIKE CAST(:text AS string) OR LOWER(e.description) LIKE CAST(:text AS string)))
              AND (:paid IS NULL OR e.paid = :paid)
              AND (CAST(:rangeStart AS timestamp) IS NULL OR e.eventDate >= :rangeStart)
              AND (CAST(:rangeEnd AS timestamp) IS NULL OR e.eventDate <= :rangeEnd)
              AND (:onlyAvailable = FALSE OR e.participantLimit = 0 OR e.participantLimit > (
                  SELECT COUNT(r.id)
                  FROM ParticipationRequest r
                  WHERE r.event.id = e.id AND r.status = ru.practicum.main.request.model.RequestStatus.CONFIRMED
              ))
              AND (:categories IS NULL OR e.category.id IN :categories)
            ORDER BY
                CASE WHEN :sort = 'EVENT_DATE' THEN e.eventDate END ASC,
                e.id ASC
            """)
    List<Event> findAllWithParams(
            @Param("text") String text,
            @Param("categories") List<Long> categories,
            @Param("paid") Boolean paid,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd,
            @Param("onlyAvailable") Boolean onlyAvailable,
            @Param("sort") String sort,
            @Param("users") List<Long> users,
            @Param("states") List<String> states,
            Pageable pageable
    );

    Optional<Event> findByIdAndState(Long id, EventState state);
}
