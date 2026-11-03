package ru.practicum.core.common.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@Configuration
@Import({TestClock.class, TestTimeController.class, TimeProvider.class})
public class TimeConfig {

    @Bean
    @Profile("!test-time")
    public Clock productionClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("test-time")
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
