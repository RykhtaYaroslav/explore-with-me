package ru.practicum.core.review.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.core.event.dto.EventRatingCount;
import ru.practicum.core.review.repository.EventReviewRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewInterserviceImpl implements ReviewInterservice {
    private final EventReviewRepository eventReviewRepository;

    @Override
    public List<EventRatingCount> getEventsRatingsByIds(List<Long> ids) {
        return eventReviewRepository.getRatingByIds(ids);
    }
}
