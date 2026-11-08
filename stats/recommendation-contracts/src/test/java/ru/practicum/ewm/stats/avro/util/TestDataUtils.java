package ru.practicum.ewm.stats.avro.util;

import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.range.LongRangeRandomizer;

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
                .randomize(Long.class, new LongRangeRandomizer(1L, 1_000_000L))
                .randomize(Double.class, new ScoreRandomizer())
                .randomize(Instant.class, new TimestampRandomizer());

        return new EasyRandom(parameters);
    }

    private static class ScoreRandomizer implements Randomizer<Double> {
        private final Random random = new Random(SEED);

        @Override
        public Double getRandomValue() {
            return random.nextDouble(0.0, 1.0);
        }
    }

    private static class TimestampRandomizer implements Randomizer<Instant> {
        private final Random random = new Random(SEED);

        @Override
        public Instant getRandomValue() {
            long timestampMillis = random.nextLong(MIN_TIMESTAMP_MILLIS, MAX_TIMESTAMP_MILLIS);
            return Instant.ofEpochMilli(timestampMillis);
        }
    }
}
