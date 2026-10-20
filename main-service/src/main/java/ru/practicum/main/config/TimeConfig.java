package ru.practicum.main.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import ru.practicum.main.time.TestClock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
public class TimeConfig {

    @Bean
    @Profile("!test")
    public Clock productionClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("test")
    public Clock testClockBean(TestClock testClock) {
        return new Clock() {

            @Override
            public ZoneId getZone() {
                return ZoneId.systemDefault();
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return testClock.instant();
            }
        };
    }
}