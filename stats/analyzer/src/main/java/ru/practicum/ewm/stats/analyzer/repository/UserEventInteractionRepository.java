package ru.practicum.ewm.stats.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionEntity;
import ru.practicum.ewm.stats.analyzer.model.UserEventInteractionId;

public interface UserEventInteractionRepository
        extends JpaRepository<UserEventInteractionEntity, UserEventInteractionId> {
}
