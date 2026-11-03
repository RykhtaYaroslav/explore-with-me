package ru.practicum.ewm.stats.proto;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.jeasy.random.EasyRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.practicum.ewm.stats.proto.util.TestDataUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ProtobufMessageRoundTripTest {
    private EasyRandom random;

    @BeforeEach
    void setUp() {
        random = TestDataUtils.createEasyRandomizer();
    }

    @Test
    @DisplayName("Preserves user action fields, action type, and timestamp after Protobuf round trip")
    void userActionPreservesFieldsEnumAndTimestampAfterRoundTrip() throws InvalidProtocolBufferException {
        UserActionProto original = random.nextObject(UserActionProto.class);

        byte[] serialized = original.toByteArray();
        UserActionProto restored = UserActionProto.parseFrom(serialized);

        assertValidRandomizedData(original);

        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getActionType()).isEqualTo(original.getActionType());
        assertThat(restored.getTimestamp()).isEqualTo(original.getTimestamp());
    }

    private void assertValidRandomizedData(UserActionProto userAction) {
        assertThat(userAction.getUserId()).isPositive();
        assertThat(userAction.getEventId()).isPositive();
        assertThat(userAction.getActionType()).isNotEqualTo(ActionTypeProto.UNRECOGNIZED);
        assertThat(toInstant(userAction.getTimestamp())).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
