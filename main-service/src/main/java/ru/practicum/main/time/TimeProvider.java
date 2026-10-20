package ru.practicum.main.time;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TimeProvider {

    private final Clock clock;

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
