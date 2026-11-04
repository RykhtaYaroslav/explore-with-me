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

        UserActionProto restored = UserActionProto.parseFrom(original.toByteArray());

        assertValidRandomizedData(original);

        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getActionType()).isEqualTo(original.getActionType());
        assertThat(restored.getTimestamp()).isEqualTo(original.getTimestamp());
    }

    @Test
    @DisplayName("Preserves user predictions request fields after Protobuf round trip")
    void userPredictionsRequestPreservesFieldsAfterRoundTrip() throws InvalidProtocolBufferException {
        UserPredictionsRequestProto original = random.nextObject(UserPredictionsRequestProto.class);

        UserPredictionsRequestProto restored = UserPredictionsRequestProto.parseFrom(original.toByteArray());

        assertValidRandomizedData(original);

        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getMaxResults()).isEqualTo(original.getMaxResults());
    }

    @Test
    @DisplayName("Preserves similar events request fields after Protobuf round trip")
    void similarEventsRequestPreservesFieldsAfterRoundTrip() throws InvalidProtocolBufferException {
        SimilarEventsRequestProto original = random.nextObject(SimilarEventsRequestProto.class);

        SimilarEventsRequestProto restored = SimilarEventsRequestProto.parseFrom(original.toByteArray());

        assertValidRandomizedData(original);
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getUserId()).isEqualTo(original.getUserId());
        assertThat(restored.getMaxResults()).isEqualTo(original.getMaxResults());
    }

    @Test
    @DisplayName("Preserves interactions count request event IDs after Protobuf round trip")
    void interactionsCountRequestPreservesEventIdsAfterRoundTrip() throws InvalidProtocolBufferException {
        InteractionsCountRequestProto original = random.nextObject(InteractionsCountRequestProto.class);

        InteractionsCountRequestProto restored = InteractionsCountRequestProto.parseFrom(original.toByteArray());

        assertValidRandomizedData(original);
        assertThat(restored.getEventIdList()).containsExactlyElementsOf(original.getEventIdList());
    }

    @Test
    @DisplayName("Preserves recommended event fields after Protobuf round trip")
    void recommendedEventPreservesFieldsAfterRoundTrip() throws InvalidProtocolBufferException {
        RecommendedEventProto original = random.nextObject(RecommendedEventProto.class);

        RecommendedEventProto restored = RecommendedEventProto.parseFrom(original.toByteArray());

        assertValidRandomizedData(original);
        assertThat(restored.getEventId()).isEqualTo(original.getEventId());
        assertThat(restored.getScore()).isEqualTo(original.getScore());
    }

    private void assertValidRandomizedData(UserActionProto userAction) {
        assertThat(userAction.getUserId()).isPositive();
        assertThat(userAction.getEventId()).isPositive();
        assertThat(userAction.getActionType()).isNotEqualTo(ActionTypeProto.UNRECOGNIZED);
        assertThat(toInstant(userAction.getTimestamp())).isAfter(Instant.parse("2020-01-01T00:00:00Z"));
    }

    private void assertValidRandomizedData(UserPredictionsRequestProto request) {
        assertThat(request.getUserId()).isPositive();
        assertThat(request.getMaxResults()).isPositive();
    }

    private void assertValidRandomizedData(SimilarEventsRequestProto request) {
        assertThat(request.getEventId()).isPositive();
        assertThat(request.getUserId()).isPositive();
        assertThat(request.getMaxResults()).isPositive();
    }

    private void assertValidRandomizedData(InteractionsCountRequestProto request) {
        assertThat(request.getEventIdList()).isNotEmpty().allMatch(eventId -> eventId > 0);
    }

    private void assertValidRandomizedData(RecommendedEventProto event) {
        assertThat(event.getEventId()).isPositive();
        assertThat(event.getScore()).isPositive();
    }

    private Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
