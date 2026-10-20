package ru.practicum.main.time;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RestController
@RequestMapping("/test/time")
@Profile("test")
@RequiredArgsConstructor
public class TestTimeController {

    private final TestClock testClock;

    @PutMapping
    public void setTime(@RequestBody LocalDateTime dateTime) {
        testClock.setTime(
                dateTime.atZone(ZoneId.systemDefault()).toInstant()
        );
    }

    @GetMapping
    public LocalDateTime getTime() {
        return LocalDateTime.ofInstant(
                testClock.instant(),
                ZoneId.systemDefault()
        );
    }
}