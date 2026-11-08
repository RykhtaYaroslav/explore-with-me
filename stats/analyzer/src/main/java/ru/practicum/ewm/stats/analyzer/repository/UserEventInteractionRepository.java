package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionId;
import ru.practicum.ewm.stats.model.response.RecommendedEvent;

import java.util.List;

public interface UserEventInteractionRepository
        extends JpaRepository<UserEventInteractionEntity, UserEventInteractionId> {
    @Query("""
            select interaction.id.eventId
            from UserEventInteractionEntity interaction
            where interaction.id.userId = :userId
            order by interaction.lastInteractionTime desc, interaction.id.eventId
            """)
    List<Long> findRecentEventIds(long userId, Pageable pageable);

    @Query("""
            select new ru.practicum.ewm.stats.model.response.RecommendedEvent(
                interaction.id.eventId, sum(interaction.weight))
            from UserEventInteractionEntity interaction
            where interaction.id.eventId in :eventIds
            group by interaction.id.eventId
            """)
    List<RecommendedEvent> sumWeightsByEventIds(List<Long> eventIds);

    @Query("""
            select interaction.id.eventId as eventId, count(interaction) as views
            from UserEventInteractionEntity interaction
            where interaction.id.eventId in :eventIds and interaction.viewed = true
            group by interaction.id.eventId
            """)
    List<EventViewCount> countViewsByEventIds(List<Long> eventIds);

    interface EventViewCount {
        long getEventId();

        long getViews();
    }
}
