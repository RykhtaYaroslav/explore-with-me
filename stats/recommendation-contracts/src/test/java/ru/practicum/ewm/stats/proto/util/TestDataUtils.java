package ru.practicum.ewm.stats.proto.util;

import com.google.protobuf.Timestamp;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;

import java.time.Instant;
import java.util.Random;

public final class TestDataUtils {
    private static final long SEED = 67L;
    private static final long MIN_TIMESTAMP_MILLIS = Instant.parse("2020-01-01T00:00:00Z").toEpochMilli();
    private static final long MAX_TIMESTAMP_MILLIS = Instant.parse("2030-01-01T00:00:00Z").toEpochMilli();

    private TestDataUtils() {
    }

    public static EasyRandom createEasyRandomizer() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .seed(SEED)
                .randomize(UserActionProto.class, new UserActionProtoRandomizer())
                .randomize(UserPredictionsRequestProto.class, new UserPredictionsRequestProtoRandomizer())
                .randomize(SimilarEventsRequestProto.class, new SimilarEventsRequestProtoRandomizer())
                .randomize(InteractionsCountRequestProto.class, new InteractionsCountRequestProtoRandomizer())
                .randomize(RecommendedEventProto.class, new RecommendedEventProtoRandomizer());

        return new EasyRandom(parameters);
    }

    private static class UserActionProtoRandomizer implements Randomizer<UserActionProto> {
        private final Random random = new Random(SEED);

        @Override
        public UserActionProto getRandomValue() {
            Instant instant = Instant.ofEpochMilli(
                    random.nextLong(MIN_TIMESTAMP_MILLIS, MAX_TIMESTAMP_MILLIS)
            );
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(instant.getEpochSecond())
                    .setNanos(instant.getNano())
                    .build();

            return UserActionProto.newBuilder()
                    .setUserId(random.nextLong(1L, 1_000_001L))
                    .setEventId(random.nextLong(1L, 1_000_001L))
                    .setActionType(ActionTypeProto.forNumber(random.nextInt(3)))
                    .setTimestamp(timestamp)
                    .build();
        }
    }

    private static class UserPredictionsRequestProtoRandomizer implements Randomizer<UserPredictionsRequestProto> {
        private final Random random = new Random(SEED);

        @Override
        public UserPredictionsRequestProto getRandomValue() {
            return UserPredictionsRequestProto.newBuilder()
                    .setUserId(random.nextLong(1L, 1_000_001L))
                    .setMaxResults(random.nextInt(1, 101))
                    .build();
        }
    }

    private static class SimilarEventsRequestProtoRandomizer implements Randomizer<SimilarEventsRequestProto> {
        private final Random random = new Random(SEED);

        @Override
        public SimilarEventsRequestProto getRandomValue() {
            return SimilarEventsRequestProto.newBuilder()
                    .setEventId(random.nextLong(1L, 1_000_001L))
                    .setUserId(random.nextLong(1L, 1_000_001L))
                    .setMaxResults(random.nextInt(1, 101))
                    .build();
        }
    }

    private static class InteractionsCountRequestProtoRandomizer implements Randomizer<InteractionsCountRequestProto> {
        private final Random random = new Random(SEED);

        @Override
        public InteractionsCountRequestProto getRandomValue() {
            InteractionsCountRequestProto.Builder builder = InteractionsCountRequestProto.newBuilder();
            int eventCount = random.nextInt(1, 11);
            for (int i = 0; i < eventCount; i++) {
                builder.addEventId(random.nextLong(1L, 1_000_001L));
            }
            return builder.build();
        }
    }

    private static class RecommendedEventProtoRandomizer implements Randomizer<RecommendedEventProto> {
        private final Random random = new Random(SEED);

        @Override
        public RecommendedEventProto getRandomValue() {
            return RecommendedEventProto.newBuilder()
                    .setEventId(random.nextLong(1L, 1_000_001L))
                    .setScore(random.nextDouble(0.01, 1.01))
                    .build();
        }
    }
}
