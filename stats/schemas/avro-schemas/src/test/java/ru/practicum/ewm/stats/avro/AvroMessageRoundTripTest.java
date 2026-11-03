package ru.practicum.ewm.stats.avro;

import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.avro.util.TestDataUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AvroMessageRoundTripTest {
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        random = TestDataUtils.createEasyRandomizer();
    }

    @Test
    @DisplayName("Preserves user action fields, action type, and timestamp after Avro round trip")
    void userActionPreservesFieldsEnumAndTimestampAfterRoundTrip() throws IOException {
        UserActionAvro original = random.nextObject(UserActionAvro.class);

        ByteBuffer serialized = original.toByteBuffer();
        UserActionAvro restored = UserActionAvro.fromByteBuffer(serialized);

        assertValidRandomizedData(original.getUserId(), original.getEventId(), original.getTimestamp());

        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getActionType()).isEqualTo(original.getActionType());
        assertThat(restored.getTimestamp()).isEqualTo(original.getTimestamp());
    }

    @Test
    @DisplayName("Preserves event similarity fields and timestamp after Avro round trip")
    void eventSimilarityPreservesFieldsAndTimestampAfterRoundTrip() throws IOException {
        EventSimilarityAvro original = random.nextObject(EventSimilarityAvro.class);

        ByteBuffer serialized = original.toByteBuffer();
        EventSimilarityAvro restored = EventSimilarityAvro.fromByteBuffer(serialized);

        assertValidRandomizedData(original.getEventA(), original.getEventB(), original.getTimestamp());

        assertThat(original.getScore()).isBetween(0.0, 1.0);
        assertThat(restored.getEventA()).isEqualTo(original.getEventA());
        assertThat(restored.getEventB()).isEqualTo(original.getEventB());
        assertThat(restored.getScore()).isEqualTo(original.getScore());
        assertThat(restored.getTimestamp()).isEqualTo(original.getTimestamp());
    }

    private void assertValidRandomizedData(long firstId, long secondId, Instant timestamp) {
        assertThat(firstId).isPositive();
        assertThat(secondId).isPositive();
        assertThat(timestamp).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
    }
}
