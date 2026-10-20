package ru.practicum.main.time;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Profile("test")
public class TestClock {

    private volatile Instant currentTime = Instant.now();

    public Instant instant() {
        return currentTime;
    }

    public void setTime(Instant time) {
        this.currentTime = time;
    }
}
